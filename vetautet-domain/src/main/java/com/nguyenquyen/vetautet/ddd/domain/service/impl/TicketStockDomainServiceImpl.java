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
    public boolean decreaseStockByAtomicUpdate(Long tickerId, int quantity) {
        return ticketStockRepository.decreaseStockByAtomicUpdate(tickerId, quantity);
    }

    @Override
    public boolean decreaseStockByPessimisticLock(Long tickerId, int quantity) {
        return ticketStockRepository.decreaseStockByPessimisticLock(tickerId, quantity);
    }

    @Override
    public boolean decreaseStockByOptimisticLockCAS(Long tickerId, int oldStockAvailable, int quantity) {
        return ticketStockRepository.decreaseStockByOptimisticLockCAS(tickerId, oldStockAvailable, quantity);
    }

    @Override
    public boolean increaseStock(Long tickerId, int quantity) {
        return ticketStockRepository.increaseStock(tickerId, quantity);
    }
}
