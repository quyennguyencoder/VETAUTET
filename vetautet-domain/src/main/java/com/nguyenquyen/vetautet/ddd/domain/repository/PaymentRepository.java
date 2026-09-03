package com.nguyenquyen.vetautet.ddd.domain.repository;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.PaymentTransaction;

public interface PaymentRepository {
    boolean save(PaymentTransaction transaction);
    boolean updateStatus(String paymentId, Integer status, String gatewayId, String url);
    PaymentTransaction findByPaymentId(String paymentId);
}
