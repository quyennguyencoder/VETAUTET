package com.nguyenquyen.vetautet.ddd.domain.service.impl;


import com.nguyenquyen.vetautet.ddd.domain.service.TickerOrderDomainService;
import org.springframework.stereotype.Service;

@Service
public class TickerOrderDomainServiceImpl implements TickerOrderDomainService {

    @Override
    public boolean decreaseStockLevel1(Long tickerId, int quantity) {
        return false;
    }

    @Override
    public boolean decreaseStockLevel2(Long tickerId, int quantity) {
        return false;
    }

    @Override
    public boolean decreaseStockLevel3CAS(Long tickerId, int oldStockAvailable, int quantity) {
        return false;
    }

    @Override
    public int getStockAvailable(Long ticketId) {
        return 0;
    }

    @Override
    public boolean increaseStock(Long tickerId, int quantity) {
        return false;
    }
}
