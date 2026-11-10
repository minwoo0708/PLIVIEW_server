package com.musicreview.server.global.exception;

import lombok.Getter;

/**
 * 서비스 로직에서 의도적으로 던지는 예외. GlobalExceptionHandler가 ErrorResponse로 바꿉니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
