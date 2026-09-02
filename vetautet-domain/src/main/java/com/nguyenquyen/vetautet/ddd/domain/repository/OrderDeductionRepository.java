package com.nguyenquyen.vetautet.ddd.domain.repository;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.TickerOrder;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderDeductionRepository {
    void insertOrder(String yearMonth, TickerOrder tickerOrder);
    List<Object[]> findAll(String yearMonth);
    Object[] findByOrderNumber(String yearMonth, String orderNumber);
    List<Object[]> findByDateRange(String yearMonth, LocalDateTime startDate, LocalDateTime endDate);

    // update status
    boolean updateOrderStatus(String yearMonth, String orderNumber, Integer status);

    // cursor-based pagination: lastId=0 → first page, lastId>0 → WHERE id < lastId
    List<Object[]> findPage(String yearMonth, long lastId, int limit);
}
