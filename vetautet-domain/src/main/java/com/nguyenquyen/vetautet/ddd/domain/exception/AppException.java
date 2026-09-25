package com.nguyenquyen.vetautet.ddd.domain.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final Integer code;
    private final String message;

    public AppException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public AppException(String message) {
        super(message);
        this.code = 400; // Default bad request
        this.message = message;
    }
}
