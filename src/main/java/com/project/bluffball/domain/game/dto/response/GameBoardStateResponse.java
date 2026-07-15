package com.project.bluffball.domain.game.dto.response;

/**
 * 스코어보드·경기 진행 상태.
 */
public record GameBoardStateResponse(
        int turnNumber,
        int totalInnings,
        int inning,
        boolean isTop,
        int homeScore,
        int awayScore,
        int balls,
        int strikes,
        int outs,
        boolean firstBase,
        boolean secondBase,
        boolean thirdBase,
        boolean initialized,
        boolean gameOver
) {
}
