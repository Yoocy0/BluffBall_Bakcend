package com.project.bluffball.global.exception;

/**
 * 인증 실패 예외 (HTTP 401).
 */
public class AuthUnauthorizedException extends UnauthorizedException {

    public AuthUnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
