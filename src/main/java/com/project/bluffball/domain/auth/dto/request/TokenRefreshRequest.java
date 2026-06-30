package com.project.bluffball.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh Token 재발급 REST 요청 DTO.
 */
public record TokenRefreshRequest(

        /** 로그인 시 발급받은 Refresh Token */
        @NotBlank
        String refreshToken
) {
}
