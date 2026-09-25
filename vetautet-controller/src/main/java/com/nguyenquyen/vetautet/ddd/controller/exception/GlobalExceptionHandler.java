package com.nguyenquyen.vetautet.ddd.controller.exception;

import com.nguyenquyen.vetautet.ddd.domain.exception.AppException;
import com.nguyenquyen.vetautet.ddd.controller.model.enums.ResultUtil;
import com.nguyenquyen.vetautet.ddd.controller.model.vo.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý ngoại lệ nghiệp vụ định nghĩa sẵn
     */
    @ExceptionHandler(AppException.class)
    public ResultMessage<?> handleAppException(AppException e) {
        log.warn("AppException: code={}, message={}", e.getCode(), e.getMessage());
        return ResultUtil.error(e.getCode(), e.getMessage());
    }

    /**
     * Xử lý lỗi validation tham số (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultMessage<?> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder errorMessage = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMessage.append(fieldError.getField())
                        .append(": ")
                        .append(fieldError.getDefaultMessage())
                        .append("; ");
        }
        log.warn("Validation Exception: {}", errorMessage);
        return ResultUtil.error(400, errorMessage.toString());
    }

    /**
     * Xử lý lỗi cấm truy cập (phân quyền)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResultMessage<?> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        return ResultUtil.error(403, "Bạn không có quyền truy cập chức năng này");
    }

    /**
     * Xử lý ngoại lệ hệ thống không mong muốn
     */
    @ExceptionHandler(Exception.class)
    public ResultMessage<?> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);
        return ResultUtil.error(500, "Lỗi hệ thống, vui lòng liên hệ admin");
    }
}
