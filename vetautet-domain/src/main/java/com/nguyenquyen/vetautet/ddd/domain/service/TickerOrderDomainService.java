package com.nguyenquyen.vetautet.ddd.domain.service;

public interface TickerOrderDomainService {

    boolean decreaseStockLevel1(Long tickerId, int quantity);
    boolean decreaseStockLevel2(Long tickerId, int quantity);
    boolean decreaseStockLevel3CAS(Long tickerId, int oldStockAvailable, int quantity);

    // get stockAvailable
    int getStockAvailable(Long ticketId);

    /**
     * Tăng số lượng tồn kho (Hoàn kho)
     *
     * @param tickerId ID của vé/sản phẩm
     * @param quantity Số lượng hoàn lại
     * @return true nếu thành công
     */
    boolean increaseStock(Long tickerId, int quantity);
}
