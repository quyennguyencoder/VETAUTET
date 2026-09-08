package com.nguyenquyen.vetautet.ddd.infrastructure.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import com.nguyenquyen.vetautet.ddd.infrastructure.config.KafkaTopicConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderProducer {

    private final KafkaTemplate<String, PlaceOrderMQMessage> kafkaTemplate;

    public void sendOrderMessage(PlaceOrderMQMessage message) {
        CompletableFuture<SendResult<String, PlaceOrderMQMessage>> future =
                kafkaTemplate.send(KafkaTopicConfig.ORDER_PLACE_TOPIC, message.getToken(), message);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("KafkaOrderProducer: failed to send token={}", message.getToken(), ex);
            } else {
                log.debug("KafkaOrderProducer: sent token={} partition={} offset={}",
                        message.getToken(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

    }

    /**
     * Outbox Publisher — row-by-row mode.
     * Gửi blocking, chờ Broker ACK tối đa 5 giây.
     * Ném exception nếu Kafka fail → caller (OutboxPublisherJob) bắt và skip row đó.
     */
    public void sendAndAwaitAck(PlaceOrderMQMessage message) throws Exception {
        kafkaTemplate.send(KafkaTopicConfig.ORDER_PLACE_TOPIC, message.getToken(), message)
                .get(5, TimeUnit.SECONDS); // chờ Broker ACK, timeout sau 5s
    }

    /**
     * Outbox Publisher — batch mode.
     * Gửi async, trả về future để caller thu thập và bulk update sau.
     */
    public CompletableFuture<SendResult<String, PlaceOrderMQMessage>> sendAsync(PlaceOrderMQMessage message) {
        return kafkaTemplate.send(KafkaTopicConfig.ORDER_PLACE_TOPIC, message.getToken(), message);
    }
}