# Phân Tích Tính Lũy Đẳng (Idempotency Mechanisms)

Trong kiến trúc phân tán và xử lý bất đồng bộ (Message Queue), mạng lưới có thể bị lỗi hoặc độ trễ khiến các hệ thống bên ngoài gửi một thông điệp (hoặc một HTTP request) nhiều lần (Duplicate Requests). 

Tính Lũy đẳng (Idempotency) là khả năng đảm bảo: **Dù một thao tác được gọi một lần hay nhiều lần, kết quả cuối cùng đối với hệ thống là như nhau**. Dự án Vetautet xử lý tính lũy đẳng ở 3 vị trí then chốt dưới đây:

---

## 1. Xử Lý Trùng Lặp Message Từ Kafka (MQ Flow)

Message Broker (Kafka) chỉ đảm bảo cơ chế phân phối **"At-least-once" (Ít nhất một lần)**. Do rebalance, lỗi mạng, hoặc producer gửi lại, một Consumer có thể nhận được cùng một sự kiện tạo đơn hàng 2 lần. Nếu không có lớp bảo vệ, kho có thể bị trừ 2 lần và sinh ra 2 hóa đơn cho 1 lượt nhấp chuột.

**Vị trí cài đặt:** Lớp `KafkaOrderConsumer` và `IdempotencyKeyJPAMapper`.

### Cách hoạt động:
Ứng dụng sử dụng một bảng cơ sở dữ liệu `idempotency_key` (với ràng buộc Unique trên cột `token`).

```java
// Trong IdempotencyKeyJPAMapper
@Modifying
@Query(
    value = "INSERT IGNORE INTO idempotency_key (token, created_at, expires_at) VALUES (:token, :createdAt, :expiresAt)",
    nativeQuery = true
)
int insertIgnore(@Param("token") String token, ...);
```

```java
// Trong KafkaOrderConsumer
@Transactional(rollbackFor = Exception.class)
public void processOrder(PlaceOrderMQMessage message) {
    // Idempotency gate — INSERT IGNORE cùng transaction với business logic.
    boolean isNew = idempotencyKeyRepository.tryInsert(token, LocalDateTime.now().plusHours(24));
    if (!isNew) {
        log.info("[IDEMPOTENCY] Duplicate skip token={}", token);
        return; // Bỏ qua an toàn
    }
    // Logic trừ kho, tạo đơn hàng tiếp diễn...
}
```

### Tại sao đây là thiết kế chuẩn (Best Practice)?
- Sử dụng lệnh **`INSERT IGNORE`** của MySQL giúp không ném ra Exception (ném Exception làm Spring Rollback transaction không cần thiết và đẩy thông điệp vào Dead Letter Queue một cách oan uổng). Thay vào đó, nó trả về số dòng thay đổi: `1` là lần đầu tiên, `0` là thông điệp trùng.
- Lệnh kiểm tra này nằm **bên trong `@Transactional` cùng với logic tạo đơn**. Nếu việc tạo đơn ở bên dưới có lỗi (VD: MySQL sập), toàn bộ giao dịch bị Rollback, bao gồm cả dòng insert `idempotency_key` vừa tạo. Lần sau Kafka gửi lại, lệnh insert vẫn sẽ thành công và chạy tiếp. Nếu tách riêng ra 2 giao dịch, hệ thống sẽ bỏ qua nhầm thông điệp do tưởng đã xử lý xong.

---

## 2. Các Tấm Chắn Trạng Thái Khi Hủy Đơn (State Checks Guard)

Khi người dùng mất kiên nhẫn và bấm nút "Hủy đơn" liên tục, hoặc khi hệ thống Timeout tự động hủy (Cronjob) vô tình chạy cùng tíc tắc với việc người dùng chủ động hủy, tính lũy đẳng cần được đảm bảo để không "hoàn tiền/hoàn vé" nhiều lần.

**Vị trí cài đặt:** `cancelOrder` và `systemCancelOrder` trong `OrderAppServiceImpl`.

### Cách hoạt động:
```java
// Logic hủy thủ công
if (order.getOrderStatus() == 2) { // 2 = CANCELLED
    log.info("Order already cancelled: {}", orderNumber);
    return true; // Trả về thành công nhưng không thực thi hoàn kho bên dưới
}

// Logic hệ thống tự hủy
if (order.getOrderStatus() != 0) { // Khác 0 (PENDING) nghĩa là đã trả tiền hoặc đã hủy
    log.info("[SYSTEM-CANCEL] Đơn hàng {} không phải Pending. Bỏ qua!", orderNumber);
    return true; 
}
```

### Tại sao lại quan trọng?
Các hàm `cancelOrder` thực hiện tăng lại tồn kho (Hoàn vé) trên cả MySQL (`increaseStock`) và Redis (`increaseStockCache`). Nếu hàm này vô tình chạy 2 lần cho cùng một mã đơn, hệ thống sẽ làm dư thừa số lượng vé (Overselling ảo). Nhờ có điều kiện lọc trạng thái + kết hợp khóa `Redisson Distributed Lock` chống chạy song song, hàm sẽ bỏ qua an toàn và luôn trả về kết quả Hủy thành công (Idempotent success) mà không sinh tác dụng phụ.

---

## 3. Tạo Bảng Tự Động Đầu Tháng (Double-check Locking DDL)

Dự án áp dụng Sharding dữ liệu (bảng `order_yyyyMM`). Đầu mỗi tháng, những Order đầu tiên sẽ kích hoạt việc tạo bảng tự động (JIT Table Creation). Khi hàng ngàn Request ập đến ở cùng miligiây đó, việc thực thi DDL liên tục gây lỗi và làm sập DB.

**Vị trí cài đặt:** `ensureTableExists` trong `OrderRepositoryImpl`.

### Cách hoạt động:

```java
private static final Map<String, Boolean> tableCreatedCache = new ConcurrentHashMap<>();

private void ensureTableExists(String yearMonth) {
    String tableName = getTableName(yearMonth);
    // 1. Kiểm tra trên RAM (Idempotency qua bộ nhớ cục bộ)
    if (tableCreatedCache.containsKey(tableName)) return;
    
    synchronized (tableCreatedCache) {
        // Double-check locking (Khóa luồng trên JVM)
        if (tableCreatedCache.containsKey(tableName)) return;
        
        try {
            // 2. MySQL Idempotency
            String sql = String.format("CREATE TABLE IF NOT EXISTS `%s` ...", tableName);
            entityManager.createNativeQuery(sql).executeUpdate();
            
            tableCreatedCache.put(tableName, true); // Lưu vào RAM
        } catch (Exception e) { /*...*/ }
    }
}
```

### Tại sao lại quan trọng?
- **Khóa 2 lớp trên JVM (Double-check Locking):** Đây là màng chắn lũy đẳng ở mức độ Application. Các luồng xử lý cùng một tiến trình chỉ thực thi câu Query 1 lần duy nhất, các Request đi sau sẽ lấy trạng thái `true` từ `ConcurrentHashMap` và đi qua nhẹ nhàng (Tránh I/O Database).
- **`CREATE TABLE IF NOT EXISTS` (Idempotency ở mức Database):** Đề phòng trường hợp dự án chạy trên nhiều Server (Cluster) không chia sẻ chung `ConcurrentHashMap`, lệnh SQL này đảm bảo bản thân Database sẽ không ném lỗi nếu bảng đã được 1 Node khác tạo xong trước đó 1 phần ngàn giây. Kết quả vẫn là bảng được sinh ra an toàn.
