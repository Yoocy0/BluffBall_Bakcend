package com.project.bluffball.domain.game.dto.response;

import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.enums.TurnResult;

import java.util.List;

/**
 * 직전 턴 판정 결과 — 재접속 시 TurnResultEvent를 놓친 경우 UI 복원용.
 */
public record GameLastTurnResultResponse(
        int turnNumber,
        TurnResult turnResult,
        int finalCoordinateNumber,
        Timing pitchTiming,
        String pitchCardName,
        List<Integer> diceResults
) {
}
