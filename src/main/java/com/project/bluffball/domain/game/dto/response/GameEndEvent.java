package com.project.bluffball.domain.game.dto.response;

/**
 * 경기 종료 WebSocket 송신 이벤트.
 */
public record GameEndEvent(
        int homeScore,
        int awayScore,
        /** 승리자 유저 ID (동점 무승부 시 null) */
        Long winnerUserId
) {
}
