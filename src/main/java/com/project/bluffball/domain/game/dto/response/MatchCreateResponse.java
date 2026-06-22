package com.project.bluffball.domain.game.dto.response;

/**
 * 경기 생성 REST API 응답 DTO.
 */
public record MatchCreateResponse(
        /** 생성된 매치 세션 ID — WebSocket 연결 경로에 사용 */
        String matchSessionId
) {
}
