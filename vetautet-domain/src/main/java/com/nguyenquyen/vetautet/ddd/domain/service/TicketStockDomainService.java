package com.nguyenquyen.vetautet.ddd.domain.service;

public interface TicketStockDomainService {
    boolean decreaseStockByAtomicUpdate(Long tickerId, int quantity);
    boolean decreaseStockByPessimisticLock(Long tickerId, int quantity);
    boolean decreaseStockByOptimisticLockCAS(Long tickerId, int oldStockAvailable, int quantity);
    boolean increaseStock(Long tickerId, int quantity);
}
