package com.project.bluffball.gametest.dto;

/**
 * 테스트용 매치 생성 API 응답.
 */
public record TestMatchCreateResponse(
        String matchSessionId,
        Long pitcherUserId,
        Long batterUserId
) {
}
