package com.project.bluffball.global.exception;

/**
 * 접근 거부 예외 (HTTP 403).
 */
public class ForbiddenException extends CustomException {

    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ForbiddenException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
