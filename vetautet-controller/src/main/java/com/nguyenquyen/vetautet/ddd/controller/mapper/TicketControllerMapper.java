package com.nguyenquyen.vetautet.ddd.controller.mapper;



import com.nguyenquyen.vetautet.ddd.application.model.command.CreateTicketCommand;
import com.nguyenquyen.vetautet.ddd.application.model.command.CreateTicketDetailCommand;
import com.nguyenquyen.vetautet.ddd.controller.dto.CreateTicketDetailRequest;
import com.nguyenquyen.vetautet.ddd.controller.dto.CreateTicketRequest;

import java.math.BigDecimal;

/**
 * Controller Layer Mapper
 *
 * Chức năng: Chuyển đổi Request DTO (HTTP) → Command (Application)
 * Quy tắc: Chỉ mapping field + type conversion giữa API format và Application format
 */
public class TicketControllerMapper {

    /**
     * CreateTicketRequest → CreateTicketCommand
     */
    public static CreateTicketCommand toCommand(CreateTicketRequest req) {
        CreateTicketCommand cmd = new CreateTicketCommand();
        cmd.setTitle(req.getName());
        cmd.setDescription(req.getDescription());
        cmd.setValidFrom(req.getStartTime());
        cmd.setValidTo(req.getEndTime());
        return cmd;
    }

    /**
     * CreateTicketDetailRequest → CreateTicketDetailCommand
     *
     * Type conversion: Long → BigDecimal (API dùng Long cho đơn giản,
     * Application dùng BigDecimal cho chính xác tài chính)
     */
    public static CreateTicketDetailCommand toDetailCommand(CreateTicketDetailRequest req) {
        CreateTicketDetailCommand cmd = new CreateTicketDetailCommand();
        cmd.setName(req.getName());
        cmd.setDescription(req.getDescription());
        cmd.setStockInitial(req.getStockInitial());
        cmd.setStockAvailable(req.getStockAvailable());
        cmd.setPriceOriginal(toBigDecimal(req.getPriceOriginal()));
        cmd.setPriceFlash(toBigDecimal(req.getPriceFlash()));
        cmd.setStockPrepared(req.getStockPrepared() != null ? req.getStockPrepared() : false);
        return cmd;
    }

    /**
     * Helper: Long → BigDecimal (null-safe)
     */
    private static BigDecimal toBigDecimal(Long value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}
