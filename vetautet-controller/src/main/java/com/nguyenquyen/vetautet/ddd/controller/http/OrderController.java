package com.nguyenquyen.vetautet.ddd.controller.http;

import com.nguyenquyen.vetautet.ddd.application.model.PagedOrdersDTO;
import com.nguyenquyen.vetautet.ddd.application.model.OrderDTO;
import com.nguyenquyen.vetautet.ddd.application.model.response.PlaceOrderResponse;
import com.nguyenquyen.vetautet.ddd.application.service.order.OrderAppService;
import com.nguyenquyen.vetautet.ddd.controller.dto.CreateBookingRequest;
import com.nguyenquyen.vetautet.ddd.controller.model.enums.ResultUtil;
import com.nguyenquyen.vetautet.ddd.controller.model.vo.ResultMessage;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderAppService orderAppService;

    /**
        Level 1,2
     */
    @GetMapping("/{ticketId}/{quantity}/order")
    public boolean orderTicketByLevel(
            @PathVariable("ticketId") Long ticketId,
            @PathVariable("quantity") int quantity
    ) {
        log.info("Controller:->orderTicketByLevel | {}, {}", ticketId, quantity);
        return orderAppService.decreaseStockLevel1(ticketId, quantity);
    }

    /**
     Level 3
     */

    @GetMapping("/{ticketId}/{quantity}/cas")
    public boolean orderTicketByLevel3(
            @PathVariable("ticketId") Long ticketId,
            @PathVariable("quantity") int quantity
    ) {
        log.info("Controller:->orderTicketByLevel3 | {}, {}", ticketId, quantity);
        return orderAppService.decreaseStockLevel3CAS(ticketId, quantity);
    }

    /**
     * Queued to order
     * @param ticketId
     * @param quantity
     * @return
     */
    @GetMapping("/{ticketId}/{quantity}/{userId}/queued")
    public boolean orderTicketByMQ(
            @PathVariable("userId") Long userId,
            @PathVariable("ticketId") Long ticketId,
            @PathVariable("quantity") int quantity
    ) {
        log.info("CALL orderTicketByMQ | {}, {}, {}", userId, ticketId, quantity);
        return orderAppService.decreaseStockQueue(userId, ticketId, quantity);
    }

    @PostMapping("/cas")
    public ResultMessage<PlaceOrderResponse> placeOrderCAS(@Valid @RequestBody CreateBookingRequest request) {
        log.info("Controller:->placeOrderCAS | ticketId={}, quantity={}", request.getTicketId(), request.getQuantity());
        try {
            PlaceOrderResponse response = orderAppService.placeOrderCAS(request.getTicketId(), request.getQuantity());
            return ResultUtil.data(response);
        } catch (Exception e) {
            log.error("placeOrderCAS: unhandled error ticketId={}", request.getTicketId(), e);
            return ResultUtil.data(PlaceOrderResponse.failed("SERVER_ERROR", "Lỗi hệ thống, vui lòng thử lại"));
        }
    }



    // V1 — load toàn bộ đơn hàng (không phân trang, dùng để so sánh)
    @GetMapping("/{userId}/list")
    public ResultMessage<List<OrderDTO>> getListOrderByUser(
            @PathVariable("userId") Long userId,
            @RequestParam("ntable") String ntable
    ) {
        log.info("Controller:->getListOrderByUser [V1] | userId={} ntable={}", userId, ntable);
        return ResultUtil.data(orderAppService.findAll(ntable));
    }

    // V2 — cursor-based pagination (50 đơn/trang, O(1) dù có 10M rows)
    @GetMapping("/{userId}/list/page")
    public ResultMessage<PagedOrdersDTO> getListOrderByUserPaged(
            @PathVariable("userId") Long userId,
            @RequestParam("ntable") String ntable,
            @RequestParam(value = "cursor", defaultValue = "0") long cursor,
            @RequestParam(value = "limit",  defaultValue = "50") int limit
    ) {
        log.info("Controller:->getListOrderByUserPaged [V2] | userId={} ntable={} cursor={} limit={}", userId, ntable, cursor, limit);
        int safeLimit = Math.min(limit, 100);
        return ResultUtil.data(orderAppService.findPage(ntable, cursor, safeLimit));
    }

    // get orderItem
    @GetMapping("/{userId}/{orderNumber}")
    public ResultMessage<OrderDTO> getOrderByUser(
            @PathVariable("userId") Long userId,
            @PathVariable("orderNumber") String orderNumber
    ) {
        log.info("Controller:->getOrderByUser | {}, {}", userId, orderNumber);
        return ResultUtil.data(orderAppService.findByOrderNumber("2025xx",orderNumber));
    }

    @PutMapping("/{userId}/{orderNumber}/cancel")
    public ResultMessage<Boolean> cancelOrder(
            @PathVariable("userId") Long userId,
            @PathVariable("orderNumber") String orderNumber
    ) {
        log.info("Controller:->cancelOrder | userId: {}, orderNumber: {}", userId, orderNumber);
        boolean result = orderAppService.cancelOrder(userId, orderNumber);
        return ResultUtil.data(result);
    }
}
