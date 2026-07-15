package com.project.bluffball.global.exception;

/**
 * 접근 거부 예외 (HTTP 403).
 */
public class AuthForbiddenException extends ForbiddenException {

    public AuthForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
