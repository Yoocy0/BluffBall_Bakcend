package com.project.bluffball.domain.game.dto.response;

/**
 * GameState 스코어보드 스냅샷 — Service 이벤트 조립용 DTO.
 */
public record GameStateSnapshot(
        int inning,
        boolean isTop,
        int homeScore,
        int awayScore,
        int balls,
        int strikes,
        int outs,
        boolean firstBase,
        boolean secondBase,
        boolean thirdBase
) {
}
