package com.nguyenquyen.vetautet.ddd.application.service.order.cache;


import com.nguyenquyen.vetautet.ddd.application.model.cache.TicketDetailCache;
import com.nguyenquyen.vetautet.ddd.application.service.ticket.cache.TicketDetailCacheServiceRefactor;
import com.nguyenquyen.vetautet.ddd.infrastructure.cache.redis.RedisInfrasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketStockCacheService {

    private static final String LUA_DEDUCT =
            "local stock = redis.call('GET', KEYS[1]); " +
            "if stock == false then return -1 end; " +
            "stock = tonumber(stock); " +
            "if (stock >= tonumber(ARGV[1])) then " +
            "   redis.call('SET', KEYS[1], stock - tonumber(ARGV[1])); " +
            "   return 1; " +
            "end; " +
            "return 0; ";

    private static final String LUA_RESTORE =
            "local stock = redis.call('GET', KEYS[1]); " +
            "if (stock) then " +
            "   redis.call('SET', KEYS[1], tonumber(stock) + tonumber(ARGV[1])); " +
            "   return 1; " +
            "end; " +
            "return 0;";

    private static final DefaultRedisScript<Long> SCRIPT_DEDUCT =
            new DefaultRedisScript<>(LUA_DEDUCT, Long.class);

    private static final DefaultRedisScript<Long> SCRIPT_RESTORE =
            new DefaultRedisScript<>(LUA_RESTORE, Long.class);

    private final TicketDetailCacheServiceRefactor ticketDetailCacheServiceRefactor;

    private final RedisInfrasService redisInfrasService;

    public boolean addStockAvailableToCache(Long ticketId) {
        // That's remember check validation(*)
        if(ticketId == null) {
            return false;
        }
        // get stock_available from mysql
        TicketDetailCache ticketDetailCache = ticketDetailCacheServiceRefactor.getTicketDetail(ticketId, null);
        if(ticketDetailCache == null || ticketDetailCache.getTicketDetail() == null) {
            return false;
        }
        String keyStockItemCache = getKeyStockItemCache(ticketId);
        log.info("get->getKeyStockItemCache() | {}, {}, {}", ticketId, keyStockItemCache,
                ticketDetailCache.getTicketDetail().getStockAvailable());
        // stockAvailable = ticketDetailCache.getTicketDetail().getStockAvailable();
        redisInfrasService.setInt(keyStockItemCache, ticketDetailCache.getTicketDetail().getStockAvailable());
        return true;
    }

    // decreaseStockCache
    public int decreaseStockCache(Long ticketId, Integer quantity) {
        // 1. Get Stock Available
        String keyStockNormal = getKeyStockItemCache(ticketId);
        int stockAvailable = redisInfrasService.getInt(keyStockNormal); // 100
        log.info("stockAvailable Normal: {}, {}, {} ", keyStockNormal, stockAvailable, String.valueOf(stockAvailable - quantity));
        // 2. Decrease Stock

        if(stockAvailable >= quantity){ // 100 > 1 = 99
            redisInfrasService.setInt(keyStockNormal, stockAvailable - quantity); // 99
            log.info("stockAvailable racing...: {}", stockAvailable - quantity);
            return 1;
        }
        return 0; // stockAvailable = 0 , quantity = 1
    }

    public int decreaseStockCacheByLUA(Long ticketId, Integer quantity) {
        String key = getKeyStockItemCache(ticketId);
        Long result = redisInfrasService.getRedisTemplate().execute(SCRIPT_DEDUCT, Collections.singletonList(key), quantity);
        return result != null ? result.intValue() : -1;
    }


    private String getKeyStockItemCache(Long ticketId) {
        return "TICKET:"+ ticketId + ":STOCK";
    }

    private String getKeyStockCacheLUA(Long ticketId){
        return "LUA:TICKET:" + ticketId + ":STOCK";
    }

    // Trả về giá hiệu lực: priceFlash nếu có, ngược lại priceOriginal. -1 nếu không tìm thấy ticket.
    public long getEffectivePrice(Long ticketId) {
        TicketDetailCache cache = ticketDetailCacheServiceRefactor.getTicketDetail(ticketId, null);
        if (cache == null || cache.getTicketDetail() == null) return -1L;
        BigDecimal flash = cache.getTicketDetail().getPriceFlash();
        BigDecimal original = cache.getTicketDetail().getPriceOriginal();
        if (flash != null && flash.compareTo(BigDecimal.ZERO) > 0) return flash.longValue();
        return original != null ? original.longValue() : -1L;
    }

    // tăng stock trong cache nếu user cancel đơn hàng trong khi diễn ra flash sale
    public boolean increaseStockCache(Long ticketId, Integer quantity) {
        String key = getKeyStockItemCache(ticketId);
        Long result = redisInfrasService.getRedisTemplate().execute(SCRIPT_RESTORE, Collections.singletonList(key), quantity);
        return result != null && result == 1;
    }
}
