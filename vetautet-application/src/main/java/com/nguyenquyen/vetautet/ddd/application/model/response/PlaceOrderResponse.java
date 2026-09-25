package com.nguyenquyen.vetautet.ddd.application.model.response;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PlaceOrderResponse {

    private String placeOrderTaskId; // Dùng lưu mã đơn hàng (orderNumber)
    private Long orderId;

    public static PlaceOrderResponse success(String placeOrderTaskId) {
        return new PlaceOrderResponse()
                .setPlaceOrderTaskId(placeOrderTaskId);
    }

    public static PlaceOrderResponse success(Long orderId) {
        return new PlaceOrderResponse()
                .setOrderId(orderId);
    }
}
