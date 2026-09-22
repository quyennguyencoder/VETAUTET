package com.nguyenquyen.vetautet.ddd.application.service.payment;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public interface PaymentAppService {
    String paymentOrder(Long userId, String orderNumber, String method, String clientIp) throws UnsupportedEncodingException;
    Map<String, String> handleVnPayIpn(Map<String, String> params);
}
