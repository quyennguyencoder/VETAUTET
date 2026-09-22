package com.nguyenquyen.vetautet.ddd.application.service.payment.impl;


import com.nguyenquyen.vetautet.ddd.application.service.payment.PaymentAppService;
import com.nguyenquyen.vetautet.ddd.infrastructure.payment.PaymentGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;


import com.nguyenquyen.vetautet.ddd.application.model.OrderDTO;
import com.nguyenquyen.vetautet.ddd.application.service.order.OrderAppService;
import lombok.RequiredArgsConstructor;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentAppServiceImpl implements PaymentAppService {

    private final PaymentGatewayService paymentGatewayService;
    private final OrderAppService orderAppService;

    @Override
    public String paymentOrder(Long userId, String orderNumber, String method, String clientIp) throws UnsupportedEncodingException {
        OrderDTO order = orderAppService.findByOrderNumber(orderNumber);
        if (order == null || order.getOrderStatus() != 0) {
            throw new RuntimeException("Đơn hàng không tồn tại hoặc đã được xử lý");
        }
        
        if ("VNPAY".equalsIgnoreCase(method)) {
            return paymentGatewayService.createPaymentUrl(orderNumber, order.getTotalAmount(), clientIp);
        }
        
        throw new RuntimeException("Phương thức thanh toán không hỗ trợ: " + method);
    }

    @Override
    public Map<String, String> handleVnPayIpn(Map<String, String> params) {
        Map<String, String> response = new HashMap<>();
        try {
            boolean isVerified = paymentGatewayService.verifyPayment(params);
            if (isVerified) {
                String orderNumber = params.get("vnp_TxnRef");
                String responseCode = params.get("vnp_ResponseCode");
                String[] parts = orderNumber.split("-");
                long timestamp = Long.parseLong(parts[parts.length - 1]);
                LocalDateTime dateTime = Instant.ofEpochMilli(timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                String yearMonth = dateTime.format(DateTimeFormatter.ofPattern("yyyyMM"));
                
                OrderDTO order = orderAppService.findByOrderNumber(orderNumber);
                if (order != null) {
                    long vnpAmount = Long.parseLong(params.get("vnp_Amount")) / 100;
                    if (order.getTotalAmount().longValue() == vnpAmount) {
                        if (order.getOrderStatus() == 0) {
                            if ("00".equals(responseCode)) {
                                orderAppService.processVnPayIpn(orderNumber, yearMonth);
                            }
                            response.put("RspCode", "00");
                            response.put("Message", "Confirm Success");
                        } else {
                            response.put("RspCode", "02");
                            response.put("Message", "Order already confirmed");
                        }
                    } else {
                        response.put("RspCode", "04");
                        response.put("Message", "Invalid Amount");
                    }
                } else {
                    response.put("RspCode", "01");
                    response.put("Message", "Order not found");
                }
            } else {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
            }
        } catch (Exception e) {
            log.error("IPN Process Exception", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }
        return response;
    }
}
