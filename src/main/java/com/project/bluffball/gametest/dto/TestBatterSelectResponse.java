package com.project.bluffball.gametest.dto;

import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.enums.TurnResult;

import java.util.List;

/**
 * 타자 선택 결과 (테스트 전용) — {@link com.project.bluffball.domain.game.dto.response.TurnResultEvent} 수준.
 */
public record TestBatterSelectResponse(
        String matchSessionId,
        double responseTimeSec,
        TurnResult turnResult,
        int finalCoordinateNumber,
        Timing pitchTiming,
        List<Integer> diceResults,
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
        int turnNumber,
        int totalInnings
) {
}
