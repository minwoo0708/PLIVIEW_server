package com.musicreview.server.global.exception;

import com.musicreview.server.global.common.TimeUtils;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        List<FieldError> errors,
        OffsetDateTime timestamp
) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return of(errorCode, message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, List<FieldError> errors) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.name(), message, errors, TimeUtils.now());
    }
}
