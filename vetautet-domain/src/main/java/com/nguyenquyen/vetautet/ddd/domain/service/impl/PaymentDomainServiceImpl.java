package com.nguyenquyen.vetautet.ddd.domain.service.impl;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.PaymentTransaction;
import com.nguyenquyen.vetautet.ddd.domain.service.PaymentDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentDomainServiceImpl implements PaymentDomainService {


    @Override
    public boolean createTransaction(PaymentTransaction transaction) {
        return false;
    }

    @Override
    public boolean updateTransactionInProgress(String paymentId, String paymentUrl) {
        return false;
    }
}
