# Phân Tích Cấu Trúc Cơ Sở Dữ Liệu & Các Chức Năng Chưa Hoàn Thiện

Dựa trên việc đối chiếu giữa tập lệnh khởi tạo Database (`environment/mysql/init/ticket_init.sql`) và mã nguồn Java (Domain, Application, Infrastructure layers), dự án **Vetautet** hiện tại đã triển khai rất tốt luồng cốt lõi (Đặt vé, Trừ kho, Hủy đơn). 

Tuy nhiên, vẫn còn một số thành phần có trong thiết kế Database nhưng **chưa được cài đặt (Not Implemented)** hoặc chỉ đang được **Mock/Stub (Làm giả)** ở phía Java. Dưới đây là danh sách chi tiết để bạn có định hướng phát triển tiếp theo:

---

## 1. Thiếu Quản Lý Chi Tiết Hành Khách (Order Details)

### Vấn đề:
Trong file `ticket_init.sql`, có định nghĩa bảng phân mảnh `order_details_yyyyMM` (VD: `order_details_202502`) với các trường như:
- `passenger_name` (Tên hành khách)
- `passenger_id` (CCCD/Hộ chiếu)
- `seat_class` (Hạng ghế: Economy, Business)
- `seat_number` (Số ghế)
- `departure_station` / `arrival_station` (Ga đi/Ga đến)

### Tình trạng Codebase:
- Hiện tại, ứng dụng chỉ mới Insert dữ liệu vào bảng tổng `order_yyyyMM` (thông qua Entity `Order`) với các trường cơ bản như `ticket_id`, `quantity`, `total_amount`.
- **Hoàn toàn KHÔNG CÓ** Entity `OrderDetail`, Repository, hay bất kỳ logic xử lý nào trong `OrderAppServiceImpl` để lưu trữ danh sách hành khách đi kèm với đơn hàng.
- **Tác động:** Hệ thống hiện tại chỉ đếm số lượng vé bán ra chứ chưa gán vé cho hành khách cụ thể (Chưa chọn được chỗ ngồi).

---

## 2. Luồng Thanh Toán Chưa Được Triển Khai (Payment Integration)

### Vấn đề:
Database đã có sẵn bảng `payment_transaction` để lưu vết thanh toán:
- `payment_method` (VNPAY, MOMO...)
- `payment_status`
- `gateway_transaction_id`

Tầng Domain cũng đã khai báo Entity `PaymentTransaction` và `PaymentRepository`. Controller cũng đã có `PaymentController`.

### Tình trạng Codebase:
- Nếu bạn mở file `PaymentAppServiceImpl.java`, phương thức `paymentOrder` hiện tại chỉ đang là một hàm rỗng (Stub):
```java
@Service
public class PaymentAppServiceImpl implements PaymentAppService {
    @Override
    public String paymentOrder(Long userId, String orderNumber, String method) throws UnsupportedEncodingException {
        return ""; // Chưa có logic tích hợp VNPay hay Momo
    }
}
```
- **Tác động:** 
  - Chưa sinh ra được URL thanh toán (Payment URL) cho client.
  - Chưa có endpoint Callback/Webhook (`GET /payment/vnpay/callback` như trong README đề cập) để cập nhật trạng thái đơn hàng (từ `0-PENDING` sang `1-PAID`). Hiện tại các đơn hàng sau khi đặt thành công sẽ rơi vào vòng lặp... bị Worker tự động hủy (Timeout Cancel) do không có ai "chuyển trạng thái" thanh toán cho chúng.

---

## 3. Chưa Có Cơ Chế Phục Hồi Dữ Liệu Kép (Dual-Write Inconsistency Recovery - SAGA/TCC)

### Vấn đề:
Hệ thống sử dụng cơ chế ghi kép (Dual-Write) giữa Redis (bộ đệm) và MySQL (nguồn chân lý). 
- Khi người dùng hủy đơn, mã nguồn sẽ tăng vé trong DB, sau đó tăng vé trong Redis.

### Tình trạng Codebase:
- Tại file `OrderAppServiceImpl.java` (hàm `cancelOrder`), tác giả để lại đoạn comment:
```java
boolean isStockRecoveredRedis = ticketStockCacheService.increaseStockCache(ticketId, quantity);
if (!isStockRecoveredRedis) {
    log.warn("Redis stock recovery failed (Inconsistency), order: {}", orderNumber);
    // Có thể ghi log lỗi ra một bảng riêng để quét bù (Retry)
    // 3. -> MQ
    // mqService.sendRecoveryStockMessage(order.getTicketId(), order.getQuantity())
    //.. Dual write
    //... TCC -> Try Confirm Cancel
}
```
- **Tác động:** Hiện tại hệ thống chỉ `log.warn` nếu Redis sập đúng lúc hoàn vé. Dự án chưa có Worker quét lỗi (Retry) cũng như chưa cài đặt Message Queue (SAGA) để bồi thường (Compensate) trong trường hợp lệch pha dữ liệu dài hạn.

---

## 4. Tóm tắt Đề Xuất Phát Triển (Next Steps)

Nếu bạn muốn hoàn thiện dự án này ở mức Production-ready, đây là những tính năng bạn cần implement:
1. **Bổ sung Entity `OrderDetail`**: Cập nhật DTO của API `POST /order` để nhận mảng danh sách `passengers`. Sửa lại Logic Sharding Table để tạo luôn bảng `order_details_yyyyMM` đi kèm với bảng Order.
2. **Tích hợp VNPay Sandbox**: Viết logic mã hóa HMAC SHA512 tạo URL thanh toán trong `PaymentAppServiceImpl`, lưu bảng `payment_transaction`. 
3. **API Webhook Thanh Toán**: Cập nhật `OrderStatus = 1` và gỡ đơn hàng khỏi Redis Timeout ZSET khi nhận được kết quả giao dịch thành công từ Cổng thanh toán.
4. **DLQ (Dead Letter Queue)**: Thêm cơ chế bắn message lỗi qua Kafka để công nhân (Worker) có thể thử hoàn lại kho Redis nếu mạng bị trục trặc tạm thời.