package com.nguyenquyen.vetautet.ddd.controller.http;


import com.nguyenquyen.vetautet.ddd.application.service.payment.PaymentAppService;
import com.nguyenquyen.vetautet.ddd.controller.model.enums.ResultUtil;
import com.nguyenquyen.vetautet.ddd.controller.model.vo.ResultMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentAppService paymentAppService;

    // 1. Tạo thanh toán
    @PostMapping("/create")
    public ResultMessage<String> paymentOrder(
            @RequestParam("orderNumber") String orderNumber,
            @RequestParam(value = "method", defaultValue = "VNPAY") String method,
            HttpServletRequest request
    ) throws UnsupportedEncodingException {
        // Tạm bỏ userId vì có thể trích xuất từ Token, nhưng ở đây dùng clientIp
        Long userId = com.nguyenquyen.vetautet.ddd.infrastructure.security.SecurityUtils.getCurrentUserId();
        String ip = request.getHeader("X-Forwarded-For");
        String clientIp = (ip != null && !ip.isEmpty()) ? ip.split(",")[0].trim() : request.getRemoteAddr();
        log.info("Controller:->paymentOrder | userId={}, orderNumber={}, method={}", userId, orderNumber, method);
        String redirectUrl = paymentAppService.paymentOrder(userId, orderNumber, method, clientIp);
        return ResultUtil.data(redirectUrl);
    }

    // 3. Webhook IPN
    @GetMapping("/vnpay/ipn")
    public Map<String, String> vnpayIpn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                fields.put(fieldName, fieldValue);
            }
        }
        return paymentAppService.handleVnPayIpn(fields);
    }
}
