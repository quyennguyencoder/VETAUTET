package com.nguyenquyen.vetautet.ddd.infrastructure.payment;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGatewayService {
    String createPaymentUrl(String orderNumber, BigDecimal amount, String clientIp);
    boolean verifyPayment(Map<String, String> params);
}
