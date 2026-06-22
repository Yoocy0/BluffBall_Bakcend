package com.project.bluffball.domain.game.dto.response;

import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.enums.TurnResult;

import java.util.List;

/**
 * 턴 결과 WebSocket 송신 이벤트.
 */
public record TurnResultEvent(
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
        boolean thirdBase
) {
}
