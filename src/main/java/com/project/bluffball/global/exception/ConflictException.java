package com.project.bluffball.global.exception;

/**
 * 상태 충돌 예외 (HTTP 409).
 */
public class ConflictException extends CustomException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConflictException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
