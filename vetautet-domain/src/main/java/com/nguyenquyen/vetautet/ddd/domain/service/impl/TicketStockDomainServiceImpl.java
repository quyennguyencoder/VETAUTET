package com.nguyenquyen.vetautet.ddd.domain.service.impl;


import com.nguyenquyen.vetautet.ddd.domain.service.TicketStockDomainService;
import org.springframework.stereotype.Service;

@Service
public class TicketStockDomainServiceImpl implements TicketStockDomainService {

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
