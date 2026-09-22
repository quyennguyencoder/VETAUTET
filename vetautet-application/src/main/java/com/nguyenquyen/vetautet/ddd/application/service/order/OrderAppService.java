package com.nguyenquyen.vetautet.ddd.application.service.order;



import com.nguyenquyen.vetautet.ddd.application.model.PagedOrdersDTO;
import com.nguyenquyen.vetautet.ddd.application.model.OrderDTO;
import com.nguyenquyen.vetautet.ddd.application.model.response.PlaceOrderResponse;

import java.util.List;

public interface OrderAppService {
    PlaceOrderResponse placeOrderCAS(Long ticketId, int quantity);
    List<OrderDTO> findAll(String yearMonth);
    OrderDTO findByOrderNumber(String orderNumber);
    PagedOrdersDTO findPage(String yearMonth, long lastId, int limit);
    boolean cancelOrder(Long userId, String orderNumber);
    boolean systemCancelOrder(String orderNumber, String yearMonth);
    boolean processVnPayIpn(String orderNumber, String yearMonth);

}
