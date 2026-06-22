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
 * <p>판정 순서: 시간 초과(존 기준 S/B) → 폭투 → 좌표 일치 → 타이밍 → 주사위</p>
 *
 * <p>주사위는 정육면체(1~6)만 사용한다. 타이밍 완벽 일치 시 2개를 굴려
 * 합산 범위는 2~12, 1칸 어긋남 시 1개(합 1~6)이다.</p>
 */
@Component
public class TurnJudgmentCalculator {

    private static final double MAX_RESPONSE_TIME_SEC = 5.0;
    private static final int DICE_SIDES = 6;

    // 타자의 좌표/타이밍 선택에 대한 결과 판단 메서드(외부 호출용)
    public TurnJudgmentResult judge(int finalCoordinateNumber,
                                    Timing pitchTiming,
                                    int batterCoordinateNumber,
                                    Timing batterTiming,
                                    double responseTimeSec,
                                    boolean finalCoordinateIsStrike) {
        // 응답 시간이 초과한 경우 -> 스윙을 하지 않은 것으로 판정(최종 좌표에 따라 스트라이크, 볼로 판정)
        if (responseTimeSec > MAX_RESPONSE_TIME_SEC) {
            return timedOut(finalCoordinateNumber, pitchTiming, finalCoordinateIsStrike);
        }

        // 최종 좌표가 0이면서 타자가 좌표 0을 선택한 경우 -> 폭투로 판정(주자가 있는 경우 진루 및 볼로 판정)
        if (finalCoordinateNumber == 0 && batterCoordinateNumber == 0) {
            return new TurnJudgmentResult(
                    TurnResult.WILD_PITCH,
                    Collections.emptyList(),
                    batterTiming,
                    finalCoordinateNumber,
                    pitchTiming,
                    false);
        }

        // 타자 선택 좌표와 최종 좌표가 다른 경우 -> 헛스윙으로 판정(스트라이크로 판정)
        if (batterCoordinateNumber != finalCoordinateNumber) {
            return strike(finalCoordinateNumber, pitchTiming, batterTiming, false);
        }

        int timingDiff = Math.abs(pitchTiming.ordinal() - batterTiming.ordinal());
        // 좌표가 맞지만 타이밍이 맞지 않은 경우 -> 헛스윙(스트라이크로 판정)
        if (timingDiff >= 2) {
            return strike(finalCoordinateNumber, pitchTiming, batterTiming, false);
        }
        // 좌표가 맞으면서 타이밍이 어긋난 경우 -> 타격 이벤트 발생(주사위 1개 부여)
        if (timingDiff == 1) {
            return withDice(finalCoordinateNumber, pitchTiming, batterTiming, 1);
        }
        // 좌표가 맞으면서 타이밍도 맞는 경우 -> 타격 이벤트 발생(주사위 2개 부여)
        return withDice(finalCoordinateNumber, pitchTiming, batterTiming, 2);
    }

    // 시간 초과 시 호출되는 메서드
    private TurnJudgmentResult timedOut(int finalCoordinateNumber,
                                        Timing pitchTiming,
                                        boolean finalCoordinateIsStrike) {
        return countByStrikeZone(finalCoordinateIsStrike, finalCoordinateNumber, pitchTiming, null, true);
    }

    /** 최종 좌표 존 기준 스트라이크/볼 카운트 판정 (스윙 미발동) */
    private TurnJudgmentResult countByStrikeZone(boolean finalCoordinateIsStrike,
                                                 int finalCoordinateNumber,
                                                 Timing pitchTiming,
                                                 Timing batterTiming,
                                                 boolean timedOut) {
        if (finalCoordinateIsStrike) {
            return strike(finalCoordinateNumber, pitchTiming, batterTiming, timedOut);
        }
        return ball(finalCoordinateNumber, pitchTiming, batterTiming, timedOut);
    }

    // 스트라이크 시에 호출하는 메서드
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

    // 볼 시에 호출하는 메서드
    private TurnJudgmentResult ball(int finalCoordinateNumber,
                                    Timing pitchTiming,
                                    Timing batterTiming,
                                    boolean timedOut) {
        return new TurnJudgmentResult(
                TurnResult.BALL,
                Collections.emptyList(),
                batterTiming,
                finalCoordinateNumber,
                pitchTiming,
                timedOut);
    }

    // 타격 이벤트 시 호출되는 주사위 메서드
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
