package com.project.bluffball.gametest.dto;

/**
 * 테스트 화면용 — 현재 경기 진행 상태 (GameProgressService 반영 후).
 */
public record TestGameStatusResponse(
        String matchSessionId,
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
        boolean gameOver,
        boolean initialized
) {
}
