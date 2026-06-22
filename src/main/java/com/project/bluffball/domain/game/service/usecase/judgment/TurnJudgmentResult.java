package com.project.bluffball.domain.game.service.usecase.judgment;

import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.enums.TurnResult;

import java.util.List;

/**
 * 타격 판정 결과 (순수 계산 결과).
 *
 * @param turnResult 스트라이크·폭투 등 즉시 확정된 판정. 주사위 부여 시(null) 블러핑 대조 전.
 */
public record TurnJudgmentResult(
        TurnResult turnResult,
        List<Integer> diceResults,
        Timing selectedTiming,
        int finalCoordinateNumber,
        Timing pitchTiming,
        boolean timedOut
) {
}
