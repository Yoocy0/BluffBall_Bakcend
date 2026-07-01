package com.project.bluffball.gametest.dto;

/**
 * 게임 테스트 WebSocket 연결용 JWT 발급 응답.
 *
 * @param accessToken  STOMP CONNECT {@code Authorization} 헤더에 사용
 * @param userId       토큰 subject — 테스트 매치 참가 유저 ID
 * @param expiresIn    Access Token 만료(초)
 */
public record GameTestWsTokenResponse(
        String accessToken,
        Long userId,
        long expiresIn
) {
}
