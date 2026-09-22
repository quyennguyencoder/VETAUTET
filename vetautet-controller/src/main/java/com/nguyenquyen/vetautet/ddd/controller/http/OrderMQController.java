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
@PreAuthorize("hasRole('ROLE_USER')")
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
        try {
            OrderQueue queue = orderMQAppService.placeOrderMQ(request.getTicketId(), request.getQuantity());
            return ResultUtil.data(toResponse(queue));
        } catch (Exception e) {
            log.error("placeOrderMQ: unhandled error ticketId={}", request.getTicketId(), e);
            return ResultUtil.data(PlaceOrderResponse.failed("SERVER_ERROR", "Lỗi hệ thống, vui lòng thử lại"));
        }
    }

    /**
     * Poll order processing status by token.
     */
    @GetMapping("/status/{token}")
    public ResultMessage<OrderQueue> getOrderStatus(@PathVariable("token") String token) {
        log.info("OrderMQController:->getOrderStatus | token={}", token);
        return ResultUtil.data(orderMQAppService.getOrderStatus(token));
    }

    private PlaceOrderResponse toResponse(OrderQueue queue) {
        if (queue.getStatus() == 2) {
            // Parse "CODE: message" from the message field
            String msg = queue.getMessage() != null ? queue.getMessage() : "SERVER_ERROR";
            String code = msg.contains(":") ? msg.substring(0, msg.indexOf(":")).trim() : "ERROR";
            return PlaceOrderResponse.failed(code, msg);
        }
        // status=0 (PENDING) → accepted into queue
        return PlaceOrderResponse.success(queue.getToken());
    }
}