package com.project.bluffball.domain.game.service.usecase.judgment;

import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.enums.TurnResult;

import java.util.List;

/**
 * 타격 판정 결과 (순수 계산 결과).
 *
 * @param turnResult            1차 즉시 확정(S/B/폭투) 또는 2차 블러핑 확정(안타 등). 1차 타격 이벤트 시 null.
 * @param diceResults           1차 타이밍 판정에서 굴린 주사위. 즉시 확정 시 빈 목록.
 * @param selectedTiming        타자가 선택한 타이밍
 * @param finalCoordinateNumber 투수 구종으로 확정된 최종 착구 좌표
 * @param pitchTiming           투수 구종 타이밍
 * @param timedOut              5초 초과로 스윙 미발동 처리됐는지
 * @param sumPenalty            2차 판정 시 주사위 합에서 뺄 페널티 (0, 3, 6 …). 즉시 확정 시 0.
 */
public record TurnJudgmentResult(
        TurnResult turnResult,
        List<Integer> diceResults,
        Timing selectedTiming,
        int finalCoordinateNumber,
        Timing pitchTiming,
        boolean timedOut,
        int sumPenalty
) {
}
