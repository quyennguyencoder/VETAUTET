package com.nguyenquyen.vetautet.ddd.domain.service.impl;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.Order;
import com.nguyenquyen.vetautet.ddd.domain.service.OrderDomainService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class OrderDomainServiceImpl implements OrderDomainService {


    @Override
    public void insertOrder(String yearMonth, Order tickerOrder) {

    }

    @Override
    public List<Object[]> findAll(String yearMonth) {
        return List.of();
    }

    @Override
    public Object[] findByOrderNumber(String yearMonth, String orderNumber) {
        return new Object[0];
    }

    @Override
    public List<Object[]> findByDateRange(String yearMonth, LocalDateTime startDate, LocalDateTime endDate) {
        return List.of();
    }

    @Override
    public boolean updateOrderStatus(String yearMonth, String orderNumber, Integer status) {
        return false;
    }

    @Override
    public List<Object[]> findPage(String yearMonth, long lastId, int limit) {
        return List.of();
    }
}
