package com.nguyenquyen.vetautet.ddd.application.cronjob;

import com.nguyenquyen.vetautet.ddd.infrastructure.mq.CancelOrderMQMessage;
import com.nguyenquyen.vetautet.ddd.infrastructure.mq.KafkaOrderProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Quét ZSET "order:cancel" mỗi vài giây, phát hiện đơn hết hạn thanh toán + đẩy sự kiện lên Kafka.
 * Đây là đường xử lý CHÍNH (gần real-time). OrderTimeoutSafetyNetJob là lưới an toàn phía sau,
 * bắt các đơn bị lọt (worker chết, Redis mất ZSET data...).
 *
 * Chỉ ZREM khỏi ZSET SAU KHI Kafka đã ACK (sendCancelMessage block chờ ACK) — nếu gửi thất bại,
 * để nguyên trong ZSET, chu kỳ sau tự retry (giống pattern OutboxPublisherJob.publishRowByRow).
 */
@Component
@Slf4j
public class OrderTimeoutWorker {

    private static final int BATCH_SIZE = 100;

    @Autowired
    private OrderCancelScheduleService orderCancelScheduleService;

    @Autowired
    private KafkaOrderProducer kafkaOrderProducer;

    @Scheduled(fixedDelay = 3000)
    public void pollAndDispatch() {
        List<String> expiredMembers = orderCancelScheduleService.pollExpired(BATCH_SIZE);
        if (expiredMembers.isEmpty()) {
            return;
        }
        log.debug("[TIMEOUT-WORKER] found {} expired orders", expiredMembers.size());

        for (String raw : expiredMembers) {
            try {
                OrderCancelScheduleService.TimeoutPayload payload =
                        orderCancelScheduleService.parse(raw);

                CancelOrderMQMessage message = new CancelOrderMQMessage(
                        payload.getOrderNumber(), payload.getYearMonth(),
                        payload.getTicketId(), payload.getQuantity(),
                        System.currentTimeMillis()
                );

                kafkaOrderProducer.sendCancelMessage(message);
                orderCancelScheduleService.remove(raw);

                log.info("[TIMEOUT-WORKER] dispatched cancel event orderNumber={}",
                        payload.getOrderNumber());
            } catch (Exception e) {
                log.error("[TIMEOUT-WORKER] failed to dispatch, will retry next cycle, raw={}",
                        raw, e);
            }
        }
    }
}