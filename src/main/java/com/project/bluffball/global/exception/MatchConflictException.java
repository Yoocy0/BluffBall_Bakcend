package com.project.bluffball.global.exception;

/**
 * 매칭 큐 상태 충돌 예외 (HTTP 409).
 */
public class MatchConflictException extends RuntimeException {

    public MatchConflictException(String message) {
        super(message);
    }
}
