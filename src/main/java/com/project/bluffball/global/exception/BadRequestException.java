package com.project.bluffball.global.exception;

/**
 * 잘못된 요청 예외 (HTTP 400).
 */
public class BadRequestException extends CustomException {

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BadRequestException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
