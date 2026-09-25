package com.nguyenquyen.vetautet.ddd.domain.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Lỗi chung
    SUCCESS(200, "Thành công"),
    BAD_REQUEST(400, "Yêu cầu không hợp lệ"),
    UNAUTHORIZED(401, "Vui lòng đăng nhập để tiếp tục"),
    FORBIDDEN(403, "Bạn không có quyền truy cập"),
    NOT_FOUND(404, "Không tìm thấy dữ liệu"),
    SYSTEM_ERROR(500, "Lỗi hệ thống, vui lòng thử lại"),

    // Lỗi Ticket / Sự kiện
    TICKET_NOT_FOUND(404, "Không tìm thấy sự kiện"),

    // Lỗi Order / Đặt vé
    OUT_OF_STOCK(400, "Hết vé, vui lòng thử lại sau"),
    PRICE_NOT_FOUND(400, "Không thể xác định giá vé"),
    STOCK_CONFLICT(409, "Đặt vé không thành công, vui lòng thử lại");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
