package com.nguyenquyen.vetautet.ddd.domain.service.impl;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.Order;
import com.nguyenquyen.vetautet.ddd.domain.repository.OrderRepository;
import com.nguyenquyen.vetautet.ddd.domain.service.OrderDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class OrderDomainServiceImpl implements OrderDomainService {
    private final OrderRepository orderRepository;

    @Override
    public void insertOrder(String yearMonth, Order order) {
        orderRepository.insertOrder(yearMonth, order);
    }

    @Override
    public List<Object[]> findAll(String yearMonth) {
        return orderRepository.findAll(yearMonth);//List.of();
    }

    @Override
    public Object[] findByOrderNumber(String yearMonth, String orderNumber) {
        return orderRepository.findByOrderNumber(yearMonth, orderNumber);//new Object[0];
    }

    @Override
    public List<Object[]> findByDateRange(String yearMonth, LocalDateTime startDate, LocalDateTime endDate) {
        return List.of();
    }

    @Override
    public boolean updateOrderStatus(String yearMonth, String orderNumber, Integer status) {
        return orderRepository.updateOrderStatus(yearMonth, orderNumber, status);
    }

    @Override
    public List<Object[]> findPage(String yearMonth, long lastId, int limit) {
        return orderRepository.findPage(yearMonth, lastId, limit);
    }
}
