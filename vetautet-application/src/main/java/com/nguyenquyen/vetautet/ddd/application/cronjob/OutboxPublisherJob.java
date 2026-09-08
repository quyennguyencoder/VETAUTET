package com.nguyenquyen.vetautet.ddd.application.cronjob;

import com.alibaba.fastjson2.JSON;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.OutboxEvent;
import com.nguyenquyen.vetautet.ddd.domain.repository.OutboxEventRepository;
import com.nguyenquyen.vetautet.ddd.infrastructure.mq.KafkaOrderProducer;
import com.nguyenquyen.vetautet.ddd.infrastructure.mq.PlaceOrderMQMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Đọc outbox_event PENDING → gửi Kafka → update PUBLISHED sau khi nhận Broker ACK.
 *
 * Hai cách triển khai:
 *   publishRowByRow() — an toàn hơn, failure window nhỏ hơn (default)
 *   publishBatch()    — throughput cao hơn, yêu cầu consumer idempotent chặt hơn
 *
 * Để đổi cách: thay publishRowByRow() thành publishBatch() trong publish().
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisherJob {

    private static final int BATCH_SIZE = 500;


    private final OutboxEventRepository outboxEventRepository;

    private final KafkaOrderProducer kafkaOrderProducer;

    // fixedDelay: chờ 1s sau khi lần trước kết thúc, không overlap
    @Scheduled(fixedDelay = 1000) // fixedRate: chạy mỗi 1s bất kể lần trước đã xong chưa (có thể overlap nếu lần trước chưa xong)
    public void publish() {
        publishRowByRow();
        // ← đổi thành publishBatch() nếu muốn chạy batch mode
    }

    // =========================================================
    // CÁCH 1: Row-by-row
    // Gửi từng message, nhận ACK, update PUBLISHED ngay lập tức.
    //
    // + Failure window nhỏ: nếu crash giữa chừng, chỉ row đang gửi bị retry
    // + Đơn giản, dễ debug: fail ở row nào, log rõ row đó
    // - Nhiều DB round trip: N messages = N lần UPDATE riêng lẻ
    // - Throughput thấp hơn batch khi có nhiều PENDING rows
    // =========================================================
    private void publishRowByRow() {
        List<OutboxEvent> events = outboxEventRepository.findPendingBatch(BATCH_SIZE); // 500 rows mỗi lần, nếu nhiều hơn sẽ phải chờ nhiều lần mới hết

        log.debug("OutboxPublisher [row-by-row]: found {} PENDING events", events.size());

        for (OutboxEvent event : events) {
            try {
                PlaceOrderMQMessage message = JSON.parseObject(event.getPayload(), PlaceOrderMQMessage.class);

                // Gửi và CHỜ Broker ACK (blocking) — chỉ sau khi Broker xác nhận mới update DB
                // Đây là Producer ACK (loại 1): broker đã nhận và persist message
                // Không liên quan đến consumer đã xử lý chưa (loại 2: consumer offset commit)
                kafkaOrderProducer.sendAndAwaitAck(message);
    // ack thành công → update status PUBLISHED ngay, giảm window failure tối đa
                outboxEventRepository.markPublished(event.getId(), LocalDateTime.now());

                log.debug("OutboxPublisher [row-by-row]: published eventId={} token={}",
                        event.getId(), event.getAggregateId());

            } catch (Exception e) {
                // Kafka fail hoặc ACK timeout → KHÔNG update status → cycle sau tự retry
                log.error("OutboxPublisher [row-by-row]: failed eventId={}, will retry next cycle",
                        event.getId(), e);
            }
        }
    }

    // =========================================================
    // CÁCH 2: Batch
    // Gửi tất cả messages async, thu thập kết quả, bulk update một lần.
    //
    // + Ít DB round trip hơn: N messages chỉ cần 1 lần UPDATE IN (ids)
    // + Throughput cao hơn khi có nhiều PENDING rows
    // - Failure window lớn hơn: crash sau khi gửi hết nhưng trước khi UPDATE
    //   → toàn bộ batch bị gửi lại → CONSUMER BẮT BUỘC PHẢI IDEMPOTENT
    // - Debug khó hơn khi partial failure (300/500 success, 200 fail)
    // =========================================================
    private void publishBatch() {
        List<OutboxEvent> events = outboxEventRepository.findPendingBatch(BATCH_SIZE);
        if (events.isEmpty()) return;

        log.debug("OutboxPublisher [batch]: processing {} PENDING events", events.size());

        // Gửi tất cả async, lưu future theo eventId
        List<Long> eventIds = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (OutboxEvent event : events) {
            try {
                PlaceOrderMQMessage message = JSON.parseObject(event.getPayload(), PlaceOrderMQMessage.class);
                CompletableFuture<Void> future = kafkaOrderProducer.sendAsync(message)
                        .thenAccept(result -> {
                            // callback chạy sau khi Broker ACK — không update DB ở đây
                            // vì sẽ bulk update bên dưới sau khi thu thập xong
                        });
                eventIds.add(event.getId());
                futures.add(future);
            } catch (Exception e) {
                log.error("OutboxPublisher [batch]: parse failed eventId={}", event.getId(), e);
            }
        }

        // Chờ tất cả futures, lọc ra những cái thành công
        List<Long> successIds = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                futures.get(i).get(); // blocking wait cho từng future
                successIds.add(eventIds.get(i));
            } catch (Exception e) {
                log.error("OutboxPublisher [batch]: kafka send failed eventId={}, will retry next cycle",
                        eventIds.get(i), e);
            }
        }

        // Một lần UPDATE duy nhất cho toàn bộ success — đây là lợi thế của batch
        if (!successIds.isEmpty()) {
            outboxEventRepository.markPublishedBatch(successIds, LocalDateTime.now());
            log.debug("OutboxPublisher [batch]: marked {} events as PUBLISHED", successIds.size());
        }
    }
}