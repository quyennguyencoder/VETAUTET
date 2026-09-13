package com.nguyenquyen.vetautet.ddd.application.cronjob;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class OrderTimeoutSafetyNetJob {

    // private final OrderDomainService orderDomainService;
    // private final KafkaOrderProducer kafkaOrderProducer;

    /**
     * Chạy mỗi 10 phút (600.000 ms) - Không cần chạy nhanh vì Redis đã lo phần chính
     */
    @Scheduled(fixedDelay = 600000)
    public void scanAndCancelOrphanOrders() {
        log.info("[SAFETY-NET] Bắt đầu quét MySQL tìm các đơn hàng quá hạn bị lọt lưới...");
        
        // 1. Tính thời gian mốc: Hiện tại trừ đi 5 phút
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
        
        // 2. Gọi hàm chọc xuống MySQL: 
        // SELECT * FROM order_202609 WHERE order_status = 0 AND created_at <= cutoffTime
        
        // 3. Vòng lặp For duyệt qua danh sách kết quả
        // Đối với mỗi đơn hàng tìm được -> Đóng gói thành CancelOrderMQMessage -> Ném lên Kafka
        
        log.info("[SAFETY-NET] Quét xong. Đã gửi yêu cầu hủy cho các đơn hàng lọt lưới.");
    }
}