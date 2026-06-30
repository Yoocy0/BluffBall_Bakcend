package com.project.bluffball.global.exception;

/**
 * 접근 거부 예외 (HTTP 403).
 *
 * <p>정지된 계정 등 권한은 있으나 접근이 차단된 경우에 사용한다.</p>
 */
public class AuthForbiddenException extends RuntimeException {

    public AuthForbiddenException(String message) {
        super(message);
    }
}
