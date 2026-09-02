package com.nguyenquyen.vetautet.ddd.application.service.order;



import com.nguyenquyen.vetautet.ddd.application.model.PagedOrdersDTO;
import com.nguyenquyen.vetautet.ddd.application.model.OrderDTO;
import com.nguyenquyen.vetautet.ddd.application.model.response.PlaceOrderResponse;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.TickerOrder;

import java.util.List;

public interface OrderAppService {

    boolean decreaseStockLevel1(Long tickerId, int quantity);
    boolean decreaseStockLevel2(Long tickerId, int quantity);
    boolean decreaseStockLevel3CAS(Long tickerId, int quantity);
    PlaceOrderResponse placeOrderCAS(Long ticketId, int quantity);

    boolean decreaseStockQueue(Long userId, Long tickerId, int quantity);

    int getStockAvailable(Long ticketId);

    // order..
    List<OrderDTO> findAll(String yearMonth);
    boolean insertOrder(String yearMonth, TickerOrder tickerOrder);
    OrderDTO findByOrderNumber(String yearMonth, String orderNumber);

    /**
     * Hủy đơn hàng và hoàn lại tồn kho trong Database + Redis
     *
     * @param userId ID của người dùng thực hiện hủy
     * @param orderNumber Mã đơn hàng (VD: OKX-SGN-1-171204...)
     * @return true nếu hủy thành công, ngược lại false
     */
    boolean cancelOrder(Long userId, String orderNumber);

    PagedOrdersDTO findPage(String yearMonth, long lastId, int limit);
}
