package com.nguyenquyen.vetautet.ddd.application.service.order.impl;


import com.alibaba.fastjson2.JSON;
import com.nguyenquyen.vetautet.ddd.application.service.order.OrderMQAppService;
import com.nguyenquyen.vetautet.ddd.application.service.order.cache.TicketStockCacheService;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.OrderQueue;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.OutboxEvent;
import com.nguyenquyen.vetautet.ddd.domain.repository.OrderQueueRepository;
import com.nguyenquyen.vetautet.ddd.domain.repository.OutboxEventRepository;
import com.nguyenquyen.vetautet.ddd.infrastructure.mq.PlaceOrderMQMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


@Service
@Slf4j
@RequiredArgsConstructor
public class OrderMQAppServiceImpl implements OrderMQAppService {

    private final TicketStockCacheService ticketStockCacheService;
    private final OrderQueueRepository orderQueueRepository;
    private final OutboxEventRepository outboxEventRepository;

    // TransactionTemplate để wrap 2 writes trong 1 transaction mà không cần @Transactional
    // (self-call trong cùng class không đi qua Spring AOP proxy nên @Transactional không hoạt động)
    private final TransactionTemplate transactionTemplate;

    @Override
    public OrderQueue placeOrderMQ(Long ticketId, int quantity) {
        // 1. Redis LUA gate — fast gate, không thay đổi so với trước
        int redisResult = ticketStockCacheService.decreaseStockCacheByLUA(ticketId, quantity);
        if (redisResult == -1) {
            log.info("placeOrderMQ: cache miss for ticketId={}, warming up...", ticketId);
            boolean warmed = ticketStockCacheService.addStockAvailableToCache(ticketId);
            if (!warmed) {
                throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.TICKET_NOT_FOUND);
            }
            redisResult = ticketStockCacheService.decreaseStockCacheByLUA(ticketId, quantity);
        }
        if (redisResult == 0) {
            log.info("placeOrderMQ: Redis OOS for ticketId={}", ticketId);
            throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.OUT_OF_STOCK);
        }

        long unitPrice = ticketStockCacheService.getEffectivePrice(ticketId);
        if (unitPrice <= 0) {
            ticketStockCacheService.increaseStockCache(ticketId, quantity);
            throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.PRICE_NOT_FOUND);
        }

        // 2. Ghi order_queue + outbox_event trong cùng 1 transaction
        // Nếu bất kỳ write nào fail → cả 2 rollback → không còn trạng thái nửa vời
        Long currentUserId = com.nguyenquyen.vetautet.ddd.infrastructure.security.SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.UNAUTHORIZED);
        }
        int userId = currentUserId.intValue();
        String token = "MQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        try {
            OrderQueue queue = transactionTemplate.execute(txStatus -> {


                OrderQueue q = new OrderQueue()
                        .setToken(token)
                        .setTicketId(ticketId.intValue())
                        .setQuantity(quantity)
                        .setUserId(userId)
                        .setStatus(0)
                        .setCreatedAt(LocalDateTime.now())
                        .setUpdatedAt(LocalDateTime.now());
                orderQueueRepository.save(q);

                // send msg to MQ via outbox pattern
                PlaceOrderMQMessage message = new PlaceOrderMQMessage(
                        token, ticketId, quantity, userId, unitPrice, System.currentTimeMillis()
                );
                //
                OutboxEvent outboxEvent = new OutboxEvent()
                        .setAggregateId(token)
                        .setEventType("ORDER_PLACED")
                        .setPayload(JSON.toJSONString(message))
                        .setStatus(0)
                        .setCreatedAt(LocalDateTime.now());
                outboxEventRepository.save(outboxEvent);

                return q;
            });

            log.info("placeOrderMQ: queued token={} ticketId={}", token, ticketId);
            return queue;

        } catch (com.nguyenquyen.vetautet.ddd.domain.exception.AppException e) {
            throw e;
        } catch (Exception e) {
            // Transaction đã rollback — compensate Redis để không trừ stock oan
            ticketStockCacheService.increaseStockCache(ticketId, quantity);
            log.error("placeOrderMQ: transaction failed, compensated Redis for ticketId={}", ticketId, e);
            throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.SYSTEM_ERROR);
        }
    }

    @Override
    public OrderQueue getOrderStatus(String token) {
        return orderQueueRepository.findByToken(token).orElse(null);
    }

    private OrderQueue failedQueue(String code, String msg) {
        return new OrderQueue()
                .setStatus(2)
                .setMessage(code + ": " + msg);
    }
}