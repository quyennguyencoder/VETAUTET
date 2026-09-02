package com.nguyenquyen.vetautet.ddd.domain.service;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.PaymentTransaction;

public interface PaymentDomainService {
    boolean createTransaction(PaymentTransaction transaction);
    boolean updateTransactionInProgress(String paymentId, String paymentUrl);
}
