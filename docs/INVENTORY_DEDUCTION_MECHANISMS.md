# Phân Tích Cơ Chế Trừ Tồn Kho (Inventory Deduction Mechanisms)

Dự án **Vetautet** giải quyết bài toán cốt lõi của các hệ thống Flash Sale: **Overselling (Bán vượt quá số lượng kho)** và **Database Overload (Quá tải cơ sở dữ liệu)**. 

Hệ thống sử dụng chiến lược **2 lớp chốt chặn (2-Tier Gating)**:
1. **Bộ đệm (Redis):** Đóng vai trò là chốt chặn luồng (Fast-Fail Gate).
2. **Cơ sở dữ liệu (MySQL):** Đóng vai trò là nguồn chân lý cuối cùng (Safety Net & Source of Truth).

Dưới đây là chi tiết mã nguồn và cách hai lớp này phối hợp, cũng như cơ chế Bù trừ (Compensation / Recovery) khi có lỗi.

---

## 1. Lớp Bộ Đệm: Trừ Kho Trên Redis (LUA Script)

Lớp `TicketStockCacheService` sử dụng Redis để lưu trữ số lượng vé khả dụng trên RAM. Thay vì dùng các lệnh `GET` và `SET` rời rạc dễ dẫn đến **Race Condition** trong môi trường đa luồng, hệ thống sử dụng **Lua Script** để đảm bảo tính nguyên tử (Atomicity). 

### 1.1. Logic Trừ Kho (`LUA_DEDUCT`)
Hệ thống sử dụng đoạn script sau để trừ vé:

```lua
local stock = redis.call('GET', KEYS[1]);
if stock == false then return -1 end; 
stock = tonumber(stock); 
if (stock >= tonumber(ARGV[1])) then 
   redis.call('SET', KEYS[1], stock - tonumber(ARGV[1])); 
   return 1; 
end; 
return 0;
```
Hàm Java sẽ đánh giá kết quả trả về từ Lua:
- **`1` (Thành công):** Đủ vé, Redis đã trừ kho. Luồng xử lý được đi tiếp xuống Database.
- **`0` (Hết vé - Out of Stock):** Ứng dụng lập tức trả lỗi `OUT_OF_STOCK` về cho người dùng (Fast-Fail). HTTP Request dừng ngay tại đây mà không hề truy vấn xuống Database, bảo vệ DB hoàn toàn khỏi lượng Request khổng lồ của Flash Sale.
- **`-1` (Cache Miss):** Lần đầu tiên truy cập, dữ liệu vé chưa được tải lên Redis. Ứng dụng gọi DB để làm ấm bộ đệm (Cache Warm-up) rồi thực hiện lại lệnh Lua trên.

### 1.2. Logic Hoàn Kho / Bù trừ (`LUA_RESTORE`)
Nếu một giao dịch ở phía sau (Database) thất bại hoặc người dùng hủy đơn, số lượng vé trên bộ nhớ đệm cần được cộng lại lập tức để người khác có thể mua:

```lua
local stock = redis.call('GET', KEYS[1]);
if (stock) then 
   redis.call('SET', KEYS[1], tonumber(stock) + tonumber(ARGV[1])); 
   return 1; 
end; 
return 0;
```

---

## 2. Lớp Database: Cơ Chế Trừ Kho Trên MySQL

Ở tầng Repository (`TicketStockJPAMapper`), hệ thống đã cài đặt sẵn **3 cơ chế khác nhau** để giải quyết bài toán trừ tồn kho đồng thời (Concurrency Control). Tuy nhiên, trên thực tế, ứng dụng **chỉ đang sử dụng một cơ chế duy nhất (Atomic Update)** để cân bằng giữa hiệu năng và độ an toàn. Hai cơ chế còn lại được triển khai mang tính chất so sánh, học thuật hoặc phục vụ Benchmark.

Dưới đây là chi tiết 3 cơ chế đang tồn tại trong mã nguồn:

### 2.1. Cơ chế đang sử dụng: Atomic Update (Cập nhật Nguyên tử)
Lợi dụng Row-Level Lock của InnoDB (MySQL), câu lệnh SQL trực tiếp giới hạn điều kiện ở vế `WHERE` (`stockAvailable >= qty`):

```sql
@Modifying
@Query("UPDATE TicketDetail t SET t.updatedAt = CURRENT_TIMESTAMP, " +
       "t.stockAvailable = t.stockAvailable - :quantity " +
       "WHERE t.id = :ticketId AND t.stockAvailable >= :quantity")
int decreaseStockByAtomicUpdate(Long ticketId, int quantity);
```
- **Lý do được chọn:** Tối ưu hóa tối đa vì không cần phải gọi 2 lệnh (`SELECT` rồi mới `UPDATE`). Database engine sẽ tự động block các thread đang tranh chấp cùng một dòng, và chỉ cập nhật nếu thỏa mãn điều kiện `stockAvailable >= quantity`.
- **Kết quả:** 
  - Trả về `1` (1 affected row): Thành công.
  - Trả về `0`: Mặc dù Redis cho qua, nhưng Database lại không còn vé (Lệch pha). Hệ thống nhận biết và gọi **Rollback kho Redis**.

### 2.2. Cơ chế chưa sử dụng: Pessimistic Lock (Khóa bi quan)
Cơ chế này sử dụng `SELECT ... FOR UPDATE` ở mức Database để khóa cứng dòng dữ liệu:

```sql
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM TicketDetail t WHERE t.id = :ticketId")
TicketDetail getTicketDetailForUpdate(Long ticketId);
```
- **Cách thức:** Thread A `SELECT ... FOR UPDATE` dòng vé số 1, toàn bộ các Thread B, C, D muốn trừ vé số 1 đều phải đứng chờ (block) cho tới khi Thread A commit Transaction. 
- **Lý do không dùng:** Rất dễ gây ra "nút thắt cổ chai" (Bottleneck) ở Database trong các đợt Flash Sale. Khi hàng ngàn Request cùng chờ một dòng lock, Database Connection Pool sẽ cạn kiệt, kéo sập toàn bộ hệ thống.

### 2.3. Cơ chế chưa sử dụng: Optimistic Lock (Khóa lạc quan bằng CAS)
Sử dụng mẫu Compare-And-Swap (CAS) dựa trên giá trị tồn kho cũ (`oldStockAvailable`):

```sql
@Modifying
@Query("UPDATE TicketDetail t SET t.updatedAt = CURRENT_TIMESTAMP, " +
       "t.stockAvailable = :oldStockAvailable - :quantity " +
       "WHERE t.id = :ticketId AND t.stockAvailable = :oldStockAvailable")
int decreaseStockByOptimisticLockCAS(Long ticketId, int oldStockAvailable, int quantity);
```
- **Cách thức:** Đọc giá trị kho hiện tại (VD: 10). Gửi lệnh update yêu cầu `WHERE stockAvailable = 10`. Nếu trong tíc tắc có Thread khác trừ vé làm kho biến thành 9, lệnh UPDATE này sẽ thất bại (affectedRows = 0).
- **Lý do không dùng:** Quá "nhạy cảm" với xung đột (High Contention). Trong môi trường Flash Sale, liên tục có update lên một vé. Nếu dùng Optimistic Lock, phần lớn Request sẽ bị lỗi (thất bại do tranh chấp) dù kho vẫn còn vé, đòi hỏi ứng dụng phải viết logic Vòng lặp (Retry-loop) rất tốn kém tài nguyên (CPU).

### 2.4. Logic Hoàn Kho (Bù trừ)
Khi người dùng hoặc hệ thống hủy đơn, vé được trả lại Database bằng câu SQL cộng trực tiếp:

```sql
@Modifying
@Query("UPDATE TicketDetail t SET t.updatedAt = CURRENT_TIMESTAMP, " +
       "t.stockAvailable = t.stockAvailable + :quantity " +
       "WHERE t.id = :ticketId")
int increaseStock(Long ticketId, int quantity);
```

---

## 3. Nghệ Thuật Phối Hợp & Cơ Chế Bù Trừ (Orchestration & Compensation)

Việc giữ đồng bộ giữa Redis (Cache) và MySQL (DB) là bài toán kinh điển (Dual-Write). Hệ thống Vetautet xử lý theo chiến lược **Bù trừ thủ công (Manual Compensation)**.

### A. Trong Luồng Đặt Vé Đồng Bộ (Sync Flow)
1. Lua script trừ kho Redis.
2. Spring thực thi Atomic Update xuống MySQL.
   - Nếu lỗi xảy ra (Network sập, MySQL trả về 0 dòng), khối `catch` hoặc khối điều kiện sẽ gọi `LUA_RESTORE` để bù trả vé ngay cho Redis. Cả DB và Redis đều được an toàn (Eventually Consistent).

### B. Trong Luồng Đặt Vé Bất Đồng Bộ (MQ Flow)
1. Lua script trừ kho Redis (Fast-Fail lấy Token nhanh).
2. Ghi Message vào `OutboxEvent` & `OrderQueue` trong 1 Local Transaction của MySQL.
   - Nếu đoạn này lỗi (Chưa kịp gửi đi đâu), khối `catch` lập tức gọi `LUA_RESTORE` bù vé Redis.
3. Message được chuyển tới `KafkaOrderConsumer`. Consumer này mới là người gọi Atomic Update (`stock - qty`) của MySQL.
   - Nếu MySQL lúc này hết hàng (Sự cố lệch pha rất hiếm gặp), Consumer sẽ gọi `LUA_RESTORE` hoàn vé cho Redis và đánh dấu `OrderQueue` = FAILED.

### C. Rủi Ro Thất Thoát Tồn Kho (Inconsistency Edge Case)
Khi hủy đơn (`cancelOrder`), hệ thống chạy theo thứ tự: 
- Khóa (Redisson Lock) -> Cập nhật trạng thái Order = Cancelled -> Cộng vé DB -> Cộng vé Redis.

**Vấn đề:** Nếu bước Cộng vé DB thành công (Transaction đã commit) nhưng bước Cộng vé Redis bị văng lỗi (Redis sập hoặc mạng lỗi), số lượng tồn kho giữa DB và Redis sẽ lệch nhau vĩnh viễn (DB dư vé, Redis báo hết).
**Khắc phục hiện hành:** Hệ thống đang ghi Log `warn` để Cảnh báo ("Redis stock recovery failed (Inconsistency)"). 
**Mở rộng tương lai:** Source code đang đề xuất hướng dùng MQ (Gửi thông điệp bù trừ qua Kafka) hoặc áp dụng mẫu thiết kế TCC (Try-Confirm-Cancel) để đảm bảo Dual-Write được nhất quán tuyệt đối.
