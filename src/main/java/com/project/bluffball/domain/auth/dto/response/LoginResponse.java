package com.project.bluffball.domain.auth.dto.response;

/**
 * 소셜 로그인 REST 응답 DTO.
 */
public record LoginResponse(

        /** JWT Access Token — 이후 REST API {@code Authorization: Bearer} 헤더에 사용 */
        String accessToken,

        /** Refresh Token — Access Token 만료 시 재발급 API에 사용 */
        String refreshToken,

        /** Access Token 만료까지 남은 시간 (초) */
        long expiresIn,

        /** 최초 로그인(자동 회원가입) 여부 */
        boolean isNewUser
) {
}
