package com.nguyenquyen.vetautet.ddd.domain.repository;

public interface TicketStockRepository {
    boolean decreaseStockByAtomicUpdate(Long tickerId, int quantity);
    boolean decreaseStockByPessimisticLock(Long tickerId, int quantity);
    boolean decreaseStockByOptimisticLockCAS(Long tickerId, int oldStockAvailable, int quantity);
    boolean increaseStock(Long tickerId, int quantity);
}
