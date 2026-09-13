package com.nguyenquyen.vetautet.ddd.application.mq;

import com.nguyenquyen.vetautet.ddd.application.service.order.OrderAppService;
import com.nguyenquyen.vetautet.ddd.infrastructure.mq.CancelOrderMQMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaCancelOrderConsumer {

    @Autowired
    private OrderAppService orderAppService;

    @KafkaListener(topics = "order-cancel-topic", groupId = "cancel-consumer-group")
    public void processCancel(CancelOrderMQMessage message) {
        log.info("Nhận lệnh hủy từ Kafka cho đơn: {}", message.getOrderNumber());
        // Gọi hàm hủy đơn hàng ở tầng Service (Xem mảnh ghép 4)
        orderAppService.systemCancelOrder(message.getOrderNumber(), message.getYearMonth());
    }
}