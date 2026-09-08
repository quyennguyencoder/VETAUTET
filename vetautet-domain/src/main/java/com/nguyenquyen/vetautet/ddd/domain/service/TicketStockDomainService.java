package com.nguyenquyen.vetautet.ddd.domain.service;

public interface TicketStockDomainService {

    boolean decreaseStockLevel1(Long tickerId, int quantity);
    boolean decreaseStockLevel2(Long tickerId, int quantity);
    boolean decreaseStockLevel3CAS(Long tickerId, int oldStockAvailable, int quantity);

    int getStockAvailable(Long ticketId);
    boolean increaseStock(Long tickerId, int quantity);
}
