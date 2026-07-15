package com.project.bluffball.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 예외 응답을 공통 {@link ErrorResponse} JSON으로 작성한다.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        HttpStatus status = errorCode.getHttpStatus();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));
    }

    public ErrorCode resolveUnauthorizedErrorCode(String authorizationHeader) {
        if (hasBearerToken(authorizationHeader)) {
            return ErrorCode.AUTH_INVALID;
        }
        return ErrorCode.AUTH_REQUIRED;
    }

    private boolean hasBearerToken(String authorizationHeader) {
        return StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX);
    }
}
