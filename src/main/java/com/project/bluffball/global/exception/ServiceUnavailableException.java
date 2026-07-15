package com.project.bluffball.global.exception;

/**
 * 외부 서비스 장애 예외 (HTTP 503).
 */
public class ServiceUnavailableException extends CustomException {

    public ServiceUnavailableException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ServiceUnavailableException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ServiceUnavailableException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }
}
