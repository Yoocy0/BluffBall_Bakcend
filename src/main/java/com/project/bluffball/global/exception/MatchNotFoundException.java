package com.project.bluffball.global.exception;

/**
 * 매칭 큐에 유저가 없을 때 발생하는 예외 (HTTP 404).
 */
public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException(String message) {
        super(message);
    }
}
