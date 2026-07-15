package com.project.bluffball.global.exception;

/**
 * API 공통 에러 응답 형식.
 */
public record ErrorResponse(
        String code,
        String message,
        int status
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.name(),
                errorCode.getMessage(),
                errorCode.getHttpStatus().value()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                errorCode.name(),
                message,
                errorCode.getHttpStatus().value()
        );
    }

    public static ErrorResponse from(CustomException ex) {
        return of(ex.getErrorCode(), ex.getMessage());
    }
}
