package com.project.bluffball.global.exception;

/**
 * 인증 실패 예외 (HTTP 401).
 */
public class UnauthorizedException extends CustomException {

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
