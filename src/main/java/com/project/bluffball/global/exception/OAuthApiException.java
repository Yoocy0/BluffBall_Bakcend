package com.project.bluffball.global.exception;

/**
 * 소셜 OAuth API 호출 실패 예외 (HTTP 503).
 */
public class OAuthApiException extends RuntimeException {

    public OAuthApiException(String message) {
        super(message);
    }

    public OAuthApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
