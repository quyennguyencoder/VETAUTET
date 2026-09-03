package com.nguyenquyen.vetautet.ddd.domain.service;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderDomainService {
    void insertOrder(String yearMonth, Order tickerOrder);
    List<Object[]> findAll(String yearMonth);
    Object[] findByOrderNumber(String yearMonth, String orderNumber);
    List<Object[]> findByDateRange(String yearMonth, LocalDateTime startDate, LocalDateTime endDate);

    // update status
    boolean updateOrderStatus(String yearMonth, String orderNumber, Integer status);

    List<Object[]> findPage(String yearMonth, long lastId, int limit);
}
