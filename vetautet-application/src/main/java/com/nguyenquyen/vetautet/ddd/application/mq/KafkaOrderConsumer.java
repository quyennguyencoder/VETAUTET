package com.nguyenquyen.vetautet.ddd.application.mq;


import com.nguyenquyen.vetautet.ddd.application.cronjob.OrderCancelScheduleService;
import com.nguyenquyen.vetautet.ddd.application.service.order.cache.TicketStockCacheService;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.Order;
import com.nguyenquyen.vetautet.ddd.domain.repository.IdempotencyKeyRepository;
import com.nguyenquyen.vetautet.ddd.domain.repository.OrderQueueRepository;
import com.nguyenquyen.vetautet.ddd.domain.service.OrderDomainService;
import com.nguyenquyen.vetautet.ddd.domain.service.TicketStockDomainService;
import com.nguyenquyen.vetautet.ddd.infrastructure.mq.PlaceOrderMQMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderConsumer {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final TicketStockDomainService ticketStockDomainService;
    private final OrderDomainService orderDomainService;
    private final TicketStockCacheService ticketStockCacheService;
    private final OrderQueueRepository orderQueueRepository;
    private final OrderCancelScheduleService orderCancelScheduleService;

    @KafkaListener(
            topics = "order-place-topic",
            groupId = "order-consumer-group",
            concurrency = "10"
    )
    @Transactional(rollbackFor = Exception.class)
    public void processOrder(PlaceOrderMQMessage message) {
        String token = message.getToken();
        Long ticketId = message.getTicketId();
        int quantity = message.getQuantity();

        // Idempotency gate — INSERT IGNORE cùng transaction với business logic.
        // Nếu exception xảy ra bên dưới: @Transactional rollback toàn bộ kể cả row này
        // → Kafka retry sẽ insert lại được (affected=1) → xử lý đúng.
        // Nếu thành công: row commit → mọi retry sau thấy affected=0 → skip.
        boolean isNew = idempotencyKeyRepository.tryInsert(token, LocalDateTime.now().plusHours(24));
        if (!isNew) {
            log.info("[IDEMPOTENCY] Duplicate skip token={}", token);
            return;
        }

        log.info("[MQ] Processing token={} ticketId={} qty={}", token, ticketId, quantity);

        boolean stockDecreased = ticketStockDomainService.decreaseStockLevel1(ticketId, quantity);
        if (!stockDecreased) {
            // Producer đã pre-deduct Redis — hoàn lại vì DB không đủ stock
            ticketStockCacheService.increaseStockCache(ticketId, quantity);
            orderQueueRepository.updateStatus(token, 2, null, "Hết vé");
            log.warn("[MQ] Out of stock token={}", token);
            return;
        }

        String orderNumber = "MQ-" + message.getUserId() + "-" + System.currentTimeMillis();
        String nTable = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        Order order = new Order();
        order.setTicketId(ticketId.intValue());
        order.setQuantity(quantity);
        order.setUserId(message.getUserId());
        order.setOrderNumber(orderNumber);
        order.setTotalAmount(new BigDecimal(message.getUnitPrice() * quantity));
        order.setOrderStatus(0);
        order.setTerminalId("MQ-SGN");
        order.setOrderNotes("MQ Order -> Pending");

        orderDomainService.insertOrder(nTable, order);
        orderQueueRepository.updateStatus(token, 1, orderNumber, null);
        // push order to SQS queue
        log.info("[MQ] Success token={} orderNumber={}", token, orderNumber);
        // return bình thường → @Transactional commit: idempotency_key + stock decrease + order + status=1

        // Đăng ký auto-cancel: nếu không thanh toán trong PAYMENT_TIMEOUT_MINUTES, OrderTimeoutWorker sẽ tự hủy
        orderCancelScheduleService.scheduleTimeout(orderNumber, nTable, ticketId.intValue(), quantity);
    }
}
