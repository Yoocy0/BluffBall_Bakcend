package com.project.bluffball.global.exception;

/**
 * 인증 실패 예외 (HTTP 401).
 *
 * <p>유효하지 않거나 만료된 Refresh Token 등에 사용한다.</p>
 */
public class AuthUnauthorizedException extends RuntimeException {

    public AuthUnauthorizedException(String message) {
        super(message);
    }
}
