package com.project.bluffball.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * REST API 공통 예외 처리.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 요청 값 규칙 위반 (400) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    /** Bean Validation 실패 (400) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    /** 인증 실패 (401) */
    @ExceptionHandler(AuthUnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(AuthUnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }

    /** 접근 거부 — 정지 계정 등 (403) */
    @ExceptionHandler(AuthForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(AuthForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
    }

    /** 소셜 OAuth API 호출 실패 (503) */
    @ExceptionHandler(OAuthApiException.class)
    public ResponseEntity<Map<String, String>> handleOAuthApi(OAuthApiException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", ex.getMessage()));
    }

    /** 매칭 큐 상태 충돌 (409) */
    @ExceptionHandler(MatchConflictException.class)
    public ResponseEntity<Map<String, String>> handleMatchConflict(MatchConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    /** 매칭 큐 등록 없음 (404) */
    @ExceptionHandler(MatchNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleMatchNotFound(MatchNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }
}
