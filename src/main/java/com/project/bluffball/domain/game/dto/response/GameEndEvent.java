package com.project.bluffball.domain.game.dto.response;

/**
 * 경기 종료 WebSocket 송신 이벤트.
 */
public record GameEndEvent(
        int homeScore,
        int awayScore,
        /** 승리자 유저 ID (몰수 등 특수 종료에서만 동점·무승부 가능, 정상 경기는 연장·끝내기로 승패 확정) */
        Long winnerUserId
) {
}
