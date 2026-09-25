package com.nguyenquyen.vetautet.ddd.controller.http;

import com.nguyenquyen.vetautet.ddd.application.model.PagedOrdersDTO;
import com.nguyenquyen.vetautet.ddd.application.model.OrderDTO;
import com.nguyenquyen.vetautet.ddd.application.model.response.PlaceOrderResponse;
import com.nguyenquyen.vetautet.ddd.application.service.order.OrderAppService;
import com.nguyenquyen.vetautet.ddd.controller.dto.CreateBookingRequest;
import com.nguyenquyen.vetautet.ddd.controller.model.enums.ResultUtil;
import com.nguyenquyen.vetautet.ddd.controller.model.vo.ResultMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.nguyenquyen.vetautet.ddd.infrastructure.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@Slf4j
@RequiredArgsConstructor
// --- BACKDOOR CHO K6 LOAD TESTING: Tạm thời tắt yêu cầu đăng nhập ---
// @PreAuthorize("hasRole('ROLE_USER')")
public class OrderController {

    private final OrderAppService orderAppService;

    @PostMapping("/cas")
    public ResultMessage<PlaceOrderResponse> placeOrderCAS(@Valid @RequestBody CreateBookingRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Controller:->placeOrderCAS | userId={}, ticketId={}, quantity={}", userId, request.getTicketId(), request.getQuantity());
        PlaceOrderResponse response = orderAppService.placeOrderCAS(request.getTicketId(), request.getQuantity());
        return ResultUtil.data(response);
    }

    // V1 — load toàn bộ đơn hàng (không phân trang, dùng để so sánh)
    @GetMapping("/list")
    public ResultMessage<List<OrderDTO>> getListOrderByUser(
            @RequestParam("ntable") String ntable
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Controller:->getListOrderByUser [V1] | userId={} ntable={}", userId, ntable);
        // TODO: Pass userId to service to only fetch their orders
        return ResultUtil.data(orderAppService.findAll(ntable));
    }

    // V2 — cursor-based pagination (50 đơn/trang, O(1) dù có 10M rows)
    @GetMapping("/list/page")
    public ResultMessage<PagedOrdersDTO> getListOrderByUserPaged(
            @RequestParam("ntable") String ntable,
            @RequestParam(value = "cursor", defaultValue = "0") long cursor,
            @RequestParam(value = "limit",  defaultValue = "50") int limit
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Controller:->getListOrderByUserPaged [V2] | userId={} ntable={} cursor={} limit={}", userId, ntable, cursor, limit);
        int safeLimit = Math.min(limit, 100);
        // TODO: Pass userId to service to only fetch their orders
        return ResultUtil.data(orderAppService.findPage(ntable, cursor, safeLimit));
    }

    // get orderItem
    @GetMapping("/{orderNumber}")
    public ResultMessage<OrderDTO> getOrderByUser(
            @PathVariable("orderNumber") String orderNumber
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderDTO order = orderAppService.findByOrderNumber(orderNumber);
        
        if (order == null) {
            throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.NOT_FOUND);
        }
        
        // --- FIX IDOR: Chặn không cho user xem đơn của người khác ---
        if (!order.getUserId().equals(userId.intValue())) {
            log.warn("IDOR attempt! User {} tried to view order {} belonging to user {}", userId, orderNumber, order.getUserId());
            throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.FORBIDDEN);
        }
        
        return ResultUtil.data(order);
    }

    @PutMapping("/{orderNumber}/cancel")
    public ResultMessage<Boolean> cancelOrder(
            @PathVariable("orderNumber") String orderNumber
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Controller:->cancelOrder | userId: {}, orderNumber: {}", userId, orderNumber);
        boolean result = orderAppService.cancelOrder(userId, orderNumber);
        return ResultUtil.data(result);
    }
}
