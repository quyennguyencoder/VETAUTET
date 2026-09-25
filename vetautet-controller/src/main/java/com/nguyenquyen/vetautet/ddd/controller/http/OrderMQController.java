package com.nguyenquyen.vetautet.ddd.controller.http;


import com.nguyenquyen.vetautet.ddd.application.model.response.PlaceOrderResponse;
import com.nguyenquyen.vetautet.ddd.application.service.order.OrderMQAppService;
import com.nguyenquyen.vetautet.ddd.controller.dto.PlaceOrderMQRequest;
import com.nguyenquyen.vetautet.ddd.controller.model.enums.ResultUtil;
import com.nguyenquyen.vetautet.ddd.controller.model.vo.ResultMessage;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.OrderQueue;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order/mq")
@Slf4j
// --- BACKDOOR CHO K6 LOAD TESTING: Tạm thời tắt yêu cầu đăng nhập ---
// @PreAuthorize("hasRole('ROLE_USER')")
public class OrderMQController {

    @Autowired
    private OrderMQAppService orderMQAppService;

    /**
     * Async order via Kafka. Returns PlaceOrderResponse (same format as /order/cas).
     * success=true  → queued, placeOrderTaskId=token
     * success=false → OOS or error, code=OUT_OF_STOCK / ...
     * Compare: k6 run flash-sale.js -e ENDPOINT=/order/mq
     */
    @PostMapping
    public ResultMessage<PlaceOrderResponse> placeOrderMQ(@Valid @RequestBody PlaceOrderMQRequest request) {
        log.info("OrderMQController:->placeOrderMQ | ticketId={} qty={}", request.getTicketId(), request.getQuantity());
        OrderQueue queue = orderMQAppService.placeOrderMQ(request.getTicketId(), request.getQuantity());
        return ResultUtil.data(PlaceOrderResponse.success(queue.getToken()));
    }

    /**
     * Poll order processing status by token.
     */
    @GetMapping("/status/{token}")
    public ResultMessage<OrderQueue> getOrderStatus(@PathVariable("token") String token) {
        log.info("OrderMQController:->getOrderStatus | token={}", token);
        return ResultUtil.data(orderMQAppService.getOrderStatus(token));
    }
}