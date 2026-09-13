package com.nguyenquyen.vetautet.ddd.application.cronjob;

import com.nguyenquyen.vetautet.ddd.application.service.order.cache.TicketStockCacheService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WarmupDataBeforeEvent {

    private final TicketStockCacheService ticketStockCacheService;

    @PostConstruct
    public void loadDataTicketItemOnce() {
        log.info("Load ticket item Once... warmup..| {}", System.currentTimeMillis());
        ticketStockCacheService.addStockAvailableToCache(3L);
    }
}
