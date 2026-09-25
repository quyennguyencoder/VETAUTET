package com.nguyenquyen.vetautet.ddd.application.service.order.impl;


import com.nguyenquyen.vetautet.ddd.application.cronjob.OrderCancelScheduleService;
import com.nguyenquyen.vetautet.ddd.application.model.OrderDTO;
import com.nguyenquyen.vetautet.ddd.application.model.PagedOrdersDTO;
import com.nguyenquyen.vetautet.ddd.application.model.response.PlaceOrderResponse;
import com.nguyenquyen.vetautet.ddd.application.service.order.OrderAppService;
import com.nguyenquyen.vetautet.ddd.application.service.order.cache.TicketStockCacheService;
import com.nguyenquyen.vetautet.ddd.domain.model.entity.Order;
import com.nguyenquyen.vetautet.ddd.domain.service.OrderDomainService;
import com.nguyenquyen.vetautet.ddd.domain.service.TicketStockDomainService;
import com.nguyenquyen.vetautet.ddd.infrastructure.distributed.redisson.RedisDistributedLocker;
import com.nguyenquyen.vetautet.ddd.infrastructure.distributed.redisson.RedisDistributedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


@Service
@Slf4j
@RequiredArgsConstructor
public class OrderAppServiceImpl implements OrderAppService {

    private static final AtomicLong ORDER_SEQ = new AtomicLong(0);

    private final TicketStockDomainService ticketStockDomainService;
    private final OrderDomainService orderDomainService;
    private final TicketStockCacheService ticketStockCacheService;
    private final RedisDistributedService redisDistributedService;
    private final OrderCancelScheduleService orderCancelScheduleService;



    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlaceOrderResponse placeOrderCAS(Long ticketId, int quantity) {
        boolean isRedisDecremented = false;
        try {
            int redisResult = ticketStockCacheService.decreaseStockCacheByLUA(ticketId, quantity);
            if (redisResult == -1) {
                log.info("placeOrderCAS: cache miss for ticketId={}, warming up...", ticketId);
                boolean warmed = ticketStockCacheService.addStockAvailableToCache(ticketId);
                if (!warmed) {
                    throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.TICKET_NOT_FOUND);
                }
                redisResult = ticketStockCacheService.decreaseStockCacheByLUA(ticketId, quantity);
            }
            if (redisResult == 0) {
                log.info("placeOrderCAS: Redis stock insufficient for ticketId={}", ticketId);
                throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.OUT_OF_STOCK);
            }
            isRedisDecremented = true;

            boolean isDecreaseStockSuccess = ticketStockDomainService.decreaseStockByAtomicUpdate(ticketId, quantity);
            if (!isDecreaseStockSuccess) {
                ticketStockCacheService.increaseStockCache(ticketId, quantity);
                log.warn("placeOrderCAS: DB update failed, rolled back Redis for ticketId={}", ticketId);
                throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.STOCK_CONFLICT);
            }

            long unitPrice = ticketStockCacheService.getEffectivePrice(ticketId);
            if (unitPrice <= 0) {
                ticketStockCacheService.increaseStockCache(ticketId, quantity);
                log.warn("placeOrderCAS: price not found for ticketId={}, rolled back Redis", ticketId);
                throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.PRICE_NOT_FOUND);
            }

            Long userId = com.nguyenquyen.vetautet.ddd.infrastructure.security.SecurityUtils.getCurrentUserId();
            if (userId == null) {
                 throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.UNAUTHORIZED);
            }
            String orderNumber = "OKX-SGN-" + userId + "-" + ORDER_SEQ.incrementAndGet() + "-" + System.currentTimeMillis();
            String nTable = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

            Order order = new Order();
            order.setTicketId(ticketId.intValue());
            order.setQuantity(quantity);
            order.setOrderStatus(0);
            order.setUserId(userId.intValue());
            order.setOrderNumber(orderNumber);
            order.setTotalAmount(new BigDecimal(unitPrice * quantity));
            order.setTerminalId("OKX-SGN");
            order.setOrderNotes("Order -> Pending");
            orderDomainService.insertOrder(nTable, order);

            orderCancelScheduleService.scheduleTimeout(orderNumber, nTable, ticketId.intValue(), quantity);

            log.info("placeOrderCAS: success | ticketId={} orderNumber={}", ticketId, orderNumber);
            return PlaceOrderResponse.success(orderNumber);

        } catch (com.nguyenquyen.vetautet.ddd.domain.exception.AppException e) {
            // Ném tiếp các lỗi nghiệp vụ đã định nghĩa
            throw e;
        } catch (Exception e) {
            log.error("placeOrderCAS: error for ticketId={}", ticketId, e);
            if (isRedisDecremented) ticketStockCacheService.increaseStockCache(ticketId, quantity);
            throw new com.nguyenquyen.vetautet.ddd.domain.exception.AppException(com.nguyenquyen.vetautet.ddd.domain.exception.ErrorCode.SYSTEM_ERROR);
        }
    }


    @Override
    public List<OrderDTO> findAll(String yearMonth) {
        // 1. Lấy dữ liệu danh sách đơn hàng từ Domain Service (Native SQL)
        List<Object[]> results = orderDomainService.findAll(yearMonth);

        // 2. Mapping lại danh sách tương ứng với DTO (đầy đủ 12 cột)
        return results.stream().map(row -> new OrderDTO(
                ((Number) row[0]).intValue(),   // id
                ((Number) row[1]).intValue(),   // user_id

                // Bổ sung các cột mới tương ứng DTO ở Bước 1 & Bước 2
                ((Number) row[2]).intValue(),   // ticket_id (Mới)
                ((Number) row[3]).intValue(),   // quantity (Mới)
                ((Number) row[4]).intValue(),   // order_status (Mới)

                (String) row[5],                // order_number (Trước là row[2])
                (BigDecimal) row[6],            // total_amount (Trước là row[3])
                (String) row[7],                // terminal_id (Trước là row[4])
                ((Timestamp) row[8]).toLocalDateTime(), // order_date (Trước là row[5])
                (String) row[9],                // order_notes (Trước là row[6])
                ((Timestamp) row[10]).toLocalDateTime(), // updated_at (Trước là row[7])
                ((Timestamp) row[11]).toLocalDateTime()  // created_at (Trước là row[8])
        )).toList();
    }

    @Override
    public OrderDTO findByOrderNumber(String orderNumber) {
        // 1. Trích xuất bảng động từ mã đơn hàng
        String nTable = extractYearMonthFromOrderNumber(orderNumber);
        log.info("nTable: findByOrderNumber = {}", nTable);
        // 2. Lấy dữ liệu thô từ Domain Service (Native Query trả về Object[])
        Object[] row = orderDomainService.findByOrderNumber(nTable, orderNumber);
        if (row == null) {
            log.warn("Order not found with number: {}", orderNumber);
            return null;
        }
        // 3. Mapping dữ liệu theo cấu trúc bảng mới (12 cột)
        // Các index được tính dựa trên thứ tự khai báo trong DDL của bạn
        return new OrderDTO(
                ((Number) row[0]).intValue(),   // id
                ((Number) row[1]).intValue(),   // userId

                // Nếu TicketOrderDTO của bạn đã được cập nhật thêm các trường:
                ((Number) row[2]).intValue(),   // ticketId (Mới)
                ((Number) row[3]).intValue(),   // quantity (Mới)
                ((Number) row[4]).intValue(),   // orderStatus (Mới)

                (String) row[5],                // orderNumber (Trước là row[2])
                (BigDecimal) row[6],            // totalAmount (Trước là row[3])
                (String) row[7],                // terminalId (Trước là row[4])
                ((Timestamp) row[8]).toLocalDateTime(), // orderDate (Trước là row[5])
                (String) row[9],                // orderNotes (Trước là row[6])
                ((Timestamp) row[10]).toLocalDateTime(), // updatedAt (Trước là row[7])
                ((Timestamp) row[11]).toLocalDateTime()  // createdAt (Trước là row[8])
        );
    }
    // chuyển đổi
    private String extractYearMonthFromOrderNumber(String orderNumber) {
        try {
            // Lấy timestamp từ orderNumber
            String[] parts = orderNumber.split("-");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid order number format");
            }
            long timestamp = Long.parseLong(parts[parts.length - 1]);

            // Chuyển đổi timestamp thành LocalDateTime
            LocalDateTime dateTime = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            // Format thành yyyyMM
            return dateTime.format(DateTimeFormatter.ofPattern("yyyyMM"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract yearMonth from orderNumber: " + orderNumber, e);
        }
    }

    @Override
    public PagedOrdersDTO findPage(String yearMonth, long lastId, int limit) {
        List<Object[]> results = orderDomainService.findPage(yearMonth, lastId, limit);
        List<OrderDTO> items = results.stream().map(row -> new OrderDTO(
                ((Number) row[0]).intValue(),
                ((Number) row[1]).intValue(),
                ((Number) row[2]).intValue(),
                ((Number) row[3]).intValue(),
                ((Number) row[4]).intValue(),
                (String) row[5],
                (java.math.BigDecimal) row[6],
                (String) row[7],
                ((java.sql.Timestamp) row[8]).toLocalDateTime(),
                (String) row[9],
                ((java.sql.Timestamp) row[10]).toLocalDateTime(),
                ((java.sql.Timestamp) row[11]).toLocalDateTime()
        )).toList();

        boolean hasMore = results.size() == limit;
        Long nextCursor = hasMore ? ((Number) results.get(results.size() - 1)[0]).longValue() : null;
        return new PagedOrdersDTO(items, nextCursor, hasMore);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long userId, String orderNumber) {
        log.info("cancelOrder | userId: {} | orderNumber: {}", userId, orderNumber);

        // 1. key Lock -> order_number
        String lockKey = "LOCK:CANCEL_ORDER:" + orderNumber;
        RedisDistributedLocker lock = redisDistributedService.getDistributedLock(lockKey);

        try {
            // keep 5 seconds
            boolean isLocked = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if(!isLocked) {
                log.warn("System is processing this order, pls wait.. {}", orderNumber); // => ELK
                return false;
            }

            // 2. Logic..
            // 2. Logic nghiệp vụ (Chỉ thực hiện sau khi đã chiếm được khóa)
            String yearMonth = extractYearMonthFromOrderNumber(orderNumber);
            OrderDTO order = findByOrderNumber( orderNumber);

            if (order == null || !order.getUserId().equals(userId.intValue())) {
                log.error("Order not found or not belong to user: {}", orderNumber);
                return false;
            }

            // Bước check quan trọng nhất: Nếu đã hủy rồi thì thoát ngay
            if (order.getOrderStatus() == 2) {
                log.info("Order already cancelled: {}", orderNumber);
                return true;
            }

            // 3. Cập nhật trạng thái trong Database
            boolean isUpdated = orderDomainService.updateOrderStatus(yearMonth, orderNumber, 2);
            if (!isUpdated) {
                log.error("Failed to update status to CANCELLED: {}", orderNumber);
                return false;
            }

            // 4. Hoàn tồn kho (Khai thác từ thông tin trong Order)
            Long ticketId = Long.valueOf(order.getTicketId());
            int quantity = order.getQuantity();

            log.info("Restoring stock: ticketId={}, quantity={}", ticketId, quantity);

            // Hoàn kho Database
            boolean isStockRecoveredDB = ticketStockDomainService.increaseStock(ticketId, quantity);
            if (!isStockRecoveredDB) {
                throw new RuntimeException("DB Stock recovery failed for order: " + orderNumber);
            }

            // Hoàn kho Redis
            boolean isStockRecoveredRedis = ticketStockCacheService.increaseStockCache(ticketId, quantity);
            if (!isStockRecoveredRedis) {
                log.warn("Redis stock recovery failed (Inconsistency), order: {}", orderNumber);
                // Có thể ghi log lỗi ra một bảng riêng để quét bù (Retry)
                // 3. -> MQ
                // mqService.sendRecoveryStockMessage(order.getTicketId(), order.getQuantity())
                //.. Dual write
                //... TCC -> Try Confirm Cancel

            }

            log.info("Cancel Order Successfully: {}", orderNumber);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            //
            lock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean systemCancelOrder(String orderNumber, String yearMonth) {
        log.info("[SYSTEM-CANCEL] Bắt đầu xử lý hủy tự động cho đơn: {}", orderNumber);
        // 1. Dùng RedisDistributedLocker khóa đơn hàng lại (Chống đua lệnh với User tự bấm hủy)
        String lockKey = "LOCK:CANCEL_ORDER:" + orderNumber;
        RedisDistributedLocker lock = redisDistributedService.getDistributedLock(lockKey);
        try {
            // Cố gắng giữ khóa trong 5 giây
            boolean isLocked = lock.tryLock(1, 5, java.util.concurrent.TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("[SYSTEM-CANCEL] Hệ thống đang bận xử lý đơn này rồi: {}", orderNumber);
                return false;
            }
            // 2. Tìm đơn hàng trong DB
            OrderDTO order = findByOrderNumber(orderNumber);
            if (order == null) {
                log.error("[SYSTEM-CANCEL] Không tìm thấy đơn hàng: {}", orderNumber);
                return false;
            }
            // BƯỚC QUAN TRỌNG: Nếu đơn đã hủy (2) hoặc đã thanh toán (1) thì THOÁT NGAY
            if (order.getOrderStatus() != 0) {
                log.info("[SYSTEM-CANCEL] Đơn hàng {} có trạng thái = {} (Không phải Pending). Bỏ qua!", orderNumber, order.getOrderStatus());
                return true;
            }
            // 3. Cập nhật trạng thái = 2 (Đã hủy)
            boolean isUpdated = orderDomainService.updateOrderStatus(yearMonth, orderNumber, 2);
            if (!isUpdated) {
                log.error("[SYSTEM-CANCEL] Lỗi Update trạng thái Database cho đơn: {}", orderNumber);
                return false;
            }
            // Lấy thông tin vé để hoàn kho
            Long ticketId = Long.valueOf(order.getTicketId());
            int quantity = order.getQuantity();
            // 4. Cộng trả vé vào MySQL
            boolean isStockRecoveredDB = ticketStockDomainService.increaseStock(ticketId, quantity);
            if (!isStockRecoveredDB) {
                throw new RuntimeException("Lỗi hoàn kho DB cho đơn: " + orderNumber);
            }
            // 5. Cộng trả vé lên Redis (RAM)
            boolean isStockRecoveredRedis = ticketStockCacheService.increaseStockCache(ticketId, quantity);
            if (!isStockRecoveredRedis) {
                log.warn("[SYSTEM-CANCEL] Hoàn vé Redis thất bại, có thể Redis đang sập. Đơn: {}", orderNumber);
            }
            log.info("[SYSTEM-CANCEL] Xử lý HỦY TỰ ĐỘNG THÀNH CÔNG đơn: {}", orderNumber);
            return true;
        } catch (Exception e) {
            log.error("[SYSTEM-CANCEL] Ngoại lệ khi hủy đơn: {}", orderNumber, e);
            throw new RuntimeException(e); // Ép Spring Rollback Transaction
        } finally {
            // Mở khóa
            lock.unlock();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean processVnPayIpn(String orderNumber, String yearMonth) {
        log.info("[VNPay-IPN] Bắt đầu xử lý thanh toán cho đơn: {}", orderNumber);
        
        String lockKey = "LOCK:PAYMENT_ORDER:" + orderNumber;
        RedisDistributedLocker lock = redisDistributedService.getDistributedLock(lockKey);
        try {
            boolean isLocked = lock.tryLock(1, 5, java.util.concurrent.TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("[VNPay-IPN] Hệ thống đang xử lý đơn này: {}", orderNumber);
                return false;
            }
            
            OrderDTO order = findByOrderNumber(orderNumber);
            if (order == null) {
                log.error("[VNPay-IPN] Không tìm thấy đơn hàng: {}", orderNumber);
                return false;
            }
            
            // Chỉ cập nhật nếu đơn đang Pending
            if (order.getOrderStatus() != 0) {
                log.info("[VNPay-IPN] Đơn hàng {} đã được xử lý (trạng thái {}). Bỏ qua!", orderNumber, order.getOrderStatus());
                return true;
            }
            
            // Cập nhật trạng thái = 1 (Thành công)
            boolean isUpdated = orderDomainService.updateOrderStatus(yearMonth, orderNumber, 1);
            if (!isUpdated) {
                log.error("[VNPay-IPN] Cập nhật trạng thái thanh toán thất bại cho đơn: {}", orderNumber);
                return false;
            }
            
            log.info("[VNPay-IPN] Thanh toán THÀNH CÔNG cho đơn: {}", orderNumber);
            return true;
        } catch (Exception e) {
            log.error("[VNPay-IPN] Lỗi xử lý thanh toán: {}", orderNumber, e);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
