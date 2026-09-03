package com.nguyenquyen.vetautet.ddd.application.service.payment.impl;


import com.nguyenquyen.vetautet.ddd.application.service.payment.PaymentAppService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;


@Service
@Slf4j
public class PaymentAppServiceImpl implements PaymentAppService {

    @Override
    public String paymentOrder(Long userId, String orderNumber, String method) throws UnsupportedEncodingException {
        return "";
    }
}
