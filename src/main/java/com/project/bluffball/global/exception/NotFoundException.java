package com.project.bluffball.global.exception;

/**
 * 리소스 없음 예외 (HTTP 404).
 */
public class NotFoundException extends CustomException {

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public NotFoundException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
