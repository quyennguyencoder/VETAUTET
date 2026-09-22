# Phân Tích Cơ Chế Cache Nhiều Lớp (Multi-layer Caching)

Trong bài toán Flash Sale (như mua vé sự kiện), ngoài việc số lượng Write (đặt vé) khổng lồ, số lượng Read (người dùng refresh trang xem vé) cũng cực kỳ lớn. Nếu để mọi truy vấn đập trực tiếp vào Database (MySQL), hệ thống sẽ sụp đổ.

Dự án **Vetautet** sử dụng kiến trúc **Cache 2 Lớp (Multi-layer Caching)** kết hợp với các cơ chế bảo vệ khỏi **Cache Stampede (Bão Cache)** và kiểm soát **Nhất quán dữ liệu (Data Consistency)**.

---

## 1. Kiến Trúc Cache 2 Lớp (L1 & L2)

Kiến trúc này được lập trình tại lớp `TicketDetailCacheServiceRefactor`.

### 1.1. L1 Cache (Local Cache - Guava)
Sử dụng thư viện **Guava CacheBuilder** để lưu trữ trực tiếp trên bộ nhớ RAM (JVM) của mỗi instance chạy ứng dụng.
- **Ưu điểm:** Tốc độ truy xuất siêu tốc (Zero-network latency), không tốn chi phí gọi mạng.
- **Cấu hình:** Sức chứa ban đầu 10, Concurrency Level 12, hết hạn (TTL) sau 5 phút.

```java
private final static Cache<Long, TicketDetailCache> ticketDetailLocalCache = CacheBuilder.newBuilder()
        .initialCapacity(10)
        .concurrencyLevel(12)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
```

### 1.2. L2 Cache (Distributed Cache - Redis)
Sử dụng **Redis** làm bộ nhớ đệm dùng chung cho toàn bộ các instance. Khi có nhiều Server chạy song song (Load Balancing), Redis đảm bảo dữ liệu đồng nhất giữa chúng.

---

## 2. Chiến Lược Nhất Quán Dữ Liệu (Data Consistency)

Vấn đề lớn nhất của L1 Cache (Local Cache) là khi Instance A cập nhật DB, Instance B không hề hay biết và tiếp tục trả về dữ liệu cũ. Dự án giải quyết việc này bằng cơ chế **Kiểm tra phiên bản (Version-Based Consistency)** do Client dẫn dắt.

### 2.1. Client-Driven Versioning
API Xem vé `GET /{ticketId}/detail/{detailId}` chấp nhận một tham số tùy chọn là `version` (thường là timestamp). 

Logic lấy dữ liệu trong `TicketDetailCacheServiceRefactor.getTicketDetail(Long ticketId, Long version)`:
1. Đọc dữ liệu từ L1 (Guava Cache).
2. Nếu `version` từ Client truyền lên **lớn hơn (mới hơn)** `version` đang có ở L1, hệ thống hiểu rằng L1 đã lỗi thời (Stale data). Ứng dụng sẽ bỏ qua L1, nhảy thẳng xuống đọc L2 (Redis).
3. Khi lấy được dữ liệu mới từ L2, hệ thống ghi đè lại vào L1.

```java
TicketDetailCache ticketDetailCache = getTicketDetailLocalCache(ticketId);
if (ticketDetailCache != null) {
    if (version == null || version <= ticketDetailCache.getVersion()) {
        return ticketDetailCache; // Dữ liệu L1 còn hợp lệ -> Trả về ngay
    }
    if (version > ticketDetailCache.getVersion()){
        return getTicketDetailDistributedCache(ticketId); // L1 cũ -> Bỏ qua, gọi Redis
    }
}
return getTicketDetailDistributedCache(ticketId);
```
**=> Ý nghĩa:** Cho phép Frontend chủ động yêu cầu dữ liệu mới nhất khi nó phát hiện (thông qua Push Notification/WebSocket hoặc hành động của người dùng) rằng vé vừa có sự thay đổi.

### 2.2. Chủ Động Hủy Cache (Active Invalidation)
Khi có sự thay đổi về dữ liệu vé, ứng dụng gọi hàm xóa để làm mới cả hai lớp:
```java
public boolean orderTicketByUser(Long ticketId) {
    ticketDetailLocalCache.invalidate(ticketId); // Xóa khỏi L1
    redisInfrasService.delete(genEventItemKey(ticketId)); // Xóa khỏi L2
    return true;
}
```

---

## 3. Cơ Chế Chống Bão Cache (Cache Stampede Prevention)

Khi một sự kiện Hot vừa mở bán, dữ liệu chưa kịp lên Cache (Cache Miss) hoặc vừa hết hạn (TTL Expired). Lúc này, hàng chục ngàn Request cùng tràn xuống MySQL, gây sập DB ngay lập tức hiện tượng này gọi là **Cache Stampede (Bão Cache)** hay Thundering Herd.

Giải pháp: Áp dụng khóa phân tán kết hợp **Double-Check Locking**.

### Cách thức hoạt động tại hàm `getTicketDetailDatabase(Long ticketId)`:

1. **Khóa Phân Tán (Redisson):**
   Thay vì để 10,000 Request cùng ập xuống MySQL, hệ thống dùng Redis Lock `tryLock(1, 5, TimeUnit.SECONDS)`. Chỉ **DUY NHẤT 1 Request** lấy được khóa mới được đi tiếp. Các request khác phải đứng đợi. Nếu quá 1 giây không đợi được khóa, trả về `null` ngay lập tức (Fast-Fail) để bảo vệ hệ thống.

2. **Kiểm Tra Lại Lần 2 (Double-Check):**
   Khi Request thứ nhất load xong DB và thả khóa, Request thứ hai (đang đợi) sẽ nhận được khóa. Thay vì ngây thơ chọc thẳng xuống DB, Request 2 sẽ kiểm tra lại L2 (Redis) một lần nữa. Vì Request 1 vừa đẩy dữ liệu mới nhất lên Redis rồi, Request 2 lấy được luôn mà không cần chạm vào DB.

```java
RedisDistributedLocker locker = redisDistributedService.getDistributedLock(genEventItemKeyLock(ticketId));
try {
    // 1. Chỉ 1 luồng được lấy khóa (Timeout đợi: 1s)
    boolean isLock = locker.tryLock(1, 5, TimeUnit.SECONDS); 
    if (!isLock) return null; // Trả lỗi/Trang trống để bảo vệ server

    // 2. Vào bên trong rồi, check lại L2 Cache một lần nữa (Double-check)
    TicketDetailCache ticketDetailCache = redisInfrasService.getObject(genEventItemKey(ticketId), TicketDetailCache.class);
    if (ticketDetailCache != null) return ticketDetailCache; // Cứu được MySQL!

    // 3. Nếu thực sự rỗng, mới Query MySQL và Push lên Redis
    TicketDetail ticketDetail = ticketDetailDomainService.getTicketDetailById(ticketId);
    ticketDetailCache = new TicketDetailCache().withClone(ticketDetail).withVersion(System.currentTimeMillis());
    redisInfrasService.setObject(genEventItemKey(ticketId), ticketDetailCache);
    return ticketDetailCache;
} finally {
    locker.unlock(); // Luôn nhả khóa
}
```

## Kết Luận Đánh Giá

1. **Hiệu năng:** L1 Cache chịu tải chính, giúp RAM phục vụ hàng triệu truy vấn mà không tốn network I/O.
2. **Khả dụng (Availability):** Cơ chế Fast-Fail `tryLock(1s)` chấp nhận việc người dùng bị trắng trang (trả về `null`) trong 1 giây đầu tiên mở bán, đổi lại hệ thống không bao giờ bị Crash.
3. **Nhất quán (Consistency):** Sự kết hợp giữa `version` Client đẩy lên và Active Invalidation giữ cho dữ liệu cuối cùng đến tay người dùng luôn chính xác mà không đòi hỏi cơ chế đồng bộ Pub/Sub phức tạp.