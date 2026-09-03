package com.nguyenquyen.vetautet.ddd.application.service.order.impl;


import com.nguyenquyen.vetautet.ddd.application.service.order.OrderMQAppService;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.OrderQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class OrderMQAppServiceImpl implements OrderMQAppService {


    @Override
    public OrderQueue placeOrderMQ(Long ticketId, int quantity) {
        return null;
    }

    @Override
    public OrderQueue getOrderStatus(String token) {
        return null;
    }
}