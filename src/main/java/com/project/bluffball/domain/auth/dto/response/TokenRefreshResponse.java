package com.project.bluffball.domain.auth.dto.response;

/**
 * Refresh Token 재발급 REST 응답 DTO.
 *
 * <p>Refresh Token Rotation 정책에 따라 새 Refresh Token도 함께 반환한다.</p>
 */
public record TokenRefreshResponse(

        /** 새로 발급된 JWT Access Token */
        String accessToken,

        /** 새로 발급된 Refresh Token (기존 토큰은 무효화) */
        String refreshToken,

        /** Access Token 만료까지 남은 시간 (초) */
        long expiresIn
) {
}
