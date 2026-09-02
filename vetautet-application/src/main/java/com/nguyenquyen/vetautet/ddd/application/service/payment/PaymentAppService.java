package com.nguyenquyen.vetautet.ddd.application.service.payment;

import java.io.UnsupportedEncodingException;

public interface PaymentAppService {
    String paymentOrder(Long userId, String orderNumber, String method) throws UnsupportedEncodingException;
}
