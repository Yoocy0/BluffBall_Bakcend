package com.project.bluffball.global.exception;

import org.springframework.http.HttpStatus;

/**
 * {@link ErrorCode} 기반 커스텀 예외 베이스.
 */
public abstract class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    protected CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected CustomException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + " " + detail);
        this.errorCode = errorCode;
    }

    protected CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    protected CustomException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getMessage() + " " + detail, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return errorCode.getHttpStatus();
    }
}
