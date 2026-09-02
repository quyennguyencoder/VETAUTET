package com.nguyenquyen.vetautet.ddd.application.service.order.impl;


import com.nguyenquyen.vetautet.ddd.application.model.OrderDTO;
import com.nguyenquyen.vetautet.ddd.application.model.PagedOrdersDTO;
import com.nguyenquyen.vetautet.ddd.application.model.response.PlaceOrderResponse;
import com.nguyenquyen.vetautet.ddd.application.service.order.OrderAppService;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.TickerOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
public class OrderAppServiceImpl implements OrderAppService {


    @Override
    public boolean decreaseStockLevel1(Long tickerId, int quantity) {
        return false;
    }

    @Override
    public boolean decreaseStockLevel2(Long tickerId, int quantity) {
        return false;
    }

    @Override
    public boolean decreaseStockLevel3CAS(Long tickerId, int quantity) {
        return false;
    }

    @Override
    public PlaceOrderResponse placeOrderCAS(Long ticketId, int quantity) {
        return null;
    }

    @Override
    public boolean decreaseStockQueue(Long userId, Long tickerId, int quantity) {
        return false;
    }

    @Override
    public int getStockAvailable(Long ticketId) {
        return 0;
    }

    @Override
    public List<OrderDTO> findAll(String yearMonth) {
        return List.of();
    }

    @Override
    public boolean insertOrder(String yearMonth, TickerOrder tickerOrder) {
        return false;
    }

    @Override
    public OrderDTO findByOrderNumber(String yearMonth, String orderNumber) {
        return null;
    }

    @Override
    public boolean cancelOrder(Long userId, String orderNumber) {
        return false;
    }

    @Override
    public PagedOrdersDTO findPage(String yearMonth, long lastId, int limit) {
        return null;
    }
}
