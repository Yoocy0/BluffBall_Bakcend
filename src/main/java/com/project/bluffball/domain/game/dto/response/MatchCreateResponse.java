package com.project.bluffball.domain.game.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 경기 생성 REST API 응답 DTO.
 * 클라이언트는 matchSessionId를 받아 WebSocket 연결 시 사용한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchCreateResponse {

    /** 생성된 매치 세션 ID — WebSocket 연결 경로에 사용 */
    private String matchSessionId;

    @Builder
    public MatchCreateResponse(String matchSessionId) {
        this.matchSessionId = matchSessionId;
    }
}
