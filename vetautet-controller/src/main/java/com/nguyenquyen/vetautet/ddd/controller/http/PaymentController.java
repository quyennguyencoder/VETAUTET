package com.nguyenquyen.vetautet.ddd.controller.http;


import com.nguyenquyen.vetautet.ddd.application.service.payment.PaymentAppService;
import com.nguyenquyen.vetautet.ddd.controller.model.enums.ResultUtil;
import com.nguyenquyen.vetautet.ddd.controller.model.vo.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/payment")
@Slf4j
public class PaymentController {
    @Autowired
    private PaymentAppService paymentAppService;
    @PostMapping("/create")
    public ResultMessage<String> paymentOrder(
            @RequestParam("userId") Long userId,
            @RequestParam("orderNumber") String orderNumber,
            @RequestParam("method") String method
    ) throws UnsupportedEncodingException {
        log.info("Controller:->paymentOrder | {}, {}, {}", userId, orderNumber, method);
        String redirectUrl = paymentAppService.paymentOrder(userId, orderNumber, method);
        return ResultUtil.data(redirectUrl);
    }
}
