package com.project.bluffball.domain.game.service.usecase.judgment;

import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.enums.TurnResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 타격 판정 순수 계산 컴포넌트 (DB·Redis 접근 없음).
 *
 * <p>판정 순서: 시간 초과 → 폭투 → 좌표 일치 → 타이밍 → 주사위</p>
 *
 * <p>주사위는 정육면체(1~6)만 사용한다. 타이밍 완벽 일치 시 2개를 굴려
 * 합산 범위는 2~12, 1칸 어긋남 시 1개(합 1~6)이다.</p>
 */
@Component
public class TurnJudgmentCalculator {

    private static final double MAX_RESPONSE_TIME_SEC = 5.0;
    private static final int DICE_SIDES = 6;

    public TurnJudgmentResult judge(int finalCoordinateNumber,
                                    Timing pitchTiming,
                                    int batterCoordinateNumber,
                                    Timing batterTiming,
                                    double responseTimeSec) {
        if (responseTimeSec > MAX_RESPONSE_TIME_SEC) {
            return timedOut(finalCoordinateNumber, pitchTiming);
        }

        if (finalCoordinateNumber == 0 && batterCoordinateNumber == 0) {
            return new TurnJudgmentResult(
                    TurnResult.WILD_PITCH,
                    Collections.emptyList(),
                    batterTiming,
                    finalCoordinateNumber,
                    pitchTiming,
                    false);
        }

        if (batterCoordinateNumber != finalCoordinateNumber) {
            return strike(finalCoordinateNumber, pitchTiming, batterTiming, false);
        }

        int timingDiff = Math.abs(pitchTiming.ordinal() - batterTiming.ordinal());
        if (timingDiff >= 2) {
            return strike(finalCoordinateNumber, pitchTiming, batterTiming, false);
        }
        if (timingDiff == 1) {
            return withDice(finalCoordinateNumber, pitchTiming, batterTiming, 1);
        }
        return withDice(finalCoordinateNumber, pitchTiming, batterTiming, 2);
    }

    private TurnJudgmentResult timedOut(int finalCoordinateNumber, Timing pitchTiming) {
        return new TurnJudgmentResult(
                TurnResult.STRIKE,
                Collections.emptyList(),
                null,
                finalCoordinateNumber,
                pitchTiming,
                true);
    }

    private TurnJudgmentResult strike(int finalCoordinateNumber,
                                      Timing pitchTiming,
                                      Timing batterTiming,
                                      boolean timedOut) {
        return new TurnJudgmentResult(
                TurnResult.STRIKE,
                Collections.emptyList(),
                batterTiming,
                finalCoordinateNumber,
                pitchTiming,
                timedOut);
    }

    private TurnJudgmentResult withDice(int finalCoordinateNumber,
                                        Timing pitchTiming,
                                        Timing batterTiming,
                                        int diceCount) {
        List<Integer> dice = rollDice(diceCount);
        return new TurnJudgmentResult(
                null,
                dice,
                batterTiming,
                finalCoordinateNumber,
                pitchTiming,
                false);
    }

    /** 정육면체 주사위를 {@code count}번 굴려 각 눈금(1~6) 목록을 반환한다. */
    private List<Integer> rollDice(int count) {
        List<Integer> results = new ArrayList<>(count);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            results.add(random.nextInt(1, DICE_SIDES + 1));
        }
        return results;
    }
}
