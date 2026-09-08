package com.nguyenquyen.vetautet.ddd.domain.service.impl;


import com.nguyenquyen.vetautet.ddd.domain.repository.TicketStockRepository;
import com.nguyenquyen.vetautet.ddd.domain.service.TicketStockDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketStockDomainServiceImpl implements TicketStockDomainService {

    private final TicketStockRepository ticketStockRepository;

    @Override
    public boolean decreaseStockLevel1(Long tickerId, int quantity) {
        return ticketStockRepository.decreaseStockLevel1(tickerId, quantity);
    }

    @Override
    public boolean decreaseStockLevel2(Long tickerId, int quantity) {
        return false;
    }

    @Override
    public boolean decreaseStockLevel3CAS(Long tickerId, int oldStockAvailable, int quantity) {
        return ticketStockRepository.decreaseStockLevel3CAS(tickerId, oldStockAvailable, quantity);
    }

    @Override
    public int getStockAvailable(Long ticketId) {
        return ticketStockRepository.getStockAvailable(ticketId);
    }

    @Override
    public boolean increaseStock(Long tickerId, int quantity) {
        return ticketStockRepository.increaseStock(tickerId, quantity);
    }
}
