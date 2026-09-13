package com.nguyenquyen.vetautet.ddd.application.cronjob;

import com.alibaba.fastjson.JSON;
import com.nguyenquyen.vetautet.ddd.infrastructure.cache.redis.RedisInfrasService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class OrderCancelScheduleService {

    private static final String ZSET_KEY = "order:cancel";

    // Thời gian chờ thanh toán trước khi tự động hủy - dùng chung cho ZADD (worker) và safety-net (DB scan)
    public static final long PAYMENT_TIMEOUT_MINUTES = 1;

    @Autowired
    private RedisInfrasService redisInfrasService;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeoutPayload {
        private String orderNumber;
        private String yearMonth;
        private Integer ticketId;
        private Integer quantity;
    }

    public void scheduleTimeout(String orderNumber, String yearMonth, Integer ticketId, Integer quantity) {
        TimeoutPayload payload = new TimeoutPayload(orderNumber, yearMonth, ticketId, quantity);
        long expireAt = System.currentTimeMillis() + PAYMENT_TIMEOUT_MINUTES * 60_000L;
        redisInfrasService.zAdd(ZSET_KEY, JSON.toJSONString(payload), expireAt);
        log.info("[CANCEL-SCHEDULE] ZADD order:cancel orderNumber={} expireAt={}", orderNumber, expireAt);
    }

    public List<String> pollExpired(int limit) {
        Set<String> members = redisInfrasService.zRangeByScore(ZSET_KEY, 0, System.currentTimeMillis(), limit);
        return new ArrayList<>(members);
    }

    public TimeoutPayload parse(String rawMember) {
        return JSON.parseObject(rawMember, TimeoutPayload.class);
    }

    public void remove(String rawMember) {
        redisInfrasService.zRemove(ZSET_KEY, rawMember);
    }
}