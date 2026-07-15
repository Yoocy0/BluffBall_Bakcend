package com.project.bluffball.global.exception;

/**
 * 소셜 OAuth API 호출 실패 예외 (HTTP 503).
 */
public class OAuthApiException extends ServiceUnavailableException {

    public OAuthApiException(ErrorCode errorCode) {
        super(errorCode);
    }

    public OAuthApiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
