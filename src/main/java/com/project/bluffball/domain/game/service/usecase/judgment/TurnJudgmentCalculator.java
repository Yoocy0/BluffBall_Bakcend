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
 * <p>판정 순서: 시간 초과(존 기준 S/B) → 폭투 → 좌표 0(스윙 미발동 S/B)
 * → 좌표(정확/상하좌우 1칸/미스) → 타이밍 → 주사위(+합 페널티)</p>
 *
 * <p>주사위는 정육면체(1~6)만 사용한다.</p>
 * <ul>
 *   <li>좌표 정확 + 타이밍 정확 → 주사위 2개</li>
 *   <li>좌표 정확 + 타이밍 ±1 → 주사위 1개</li>
 *   <li>좌표 상하좌우 ±1 + 타이밍 정확 → 주사위 1개</li>
 *   <li>좌표 상하좌우 ±1 + 타이밍 ±1 → 주사위 1개, 합 −3</li>
 *   <li>볼존 접촉 → 합 −3 (위와 중첩 가능)</li>
 * </ul>
 */
@Component
public class TurnJudgmentCalculator {

    private static final double MAX_RESPONSE_TIME_SEC = 5.0;
    private static final int DICE_SIDES = 6;
    private static final int GRID_SIZE = 5;
    private static final int SOFT_CONTACT_PENALTY = 3;

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
                    false,
                    0);
        }

        // 타자가 좌표 0을 선택한 경우 -> 스윙 미발동, 최종 좌표 존 기준 S/B
        if (batterCoordinateNumber == 0) {
            return countByStrikeZone(finalCoordinateIsStrike, finalCoordinateNumber, pitchTiming, batterTiming, false);
        }

        int coordDiff = orthogonalDistance(batterCoordinateNumber, finalCoordinateNumber);
        // 좌표가 2칸 이상 떨어지거나 격자 밖이면 헛스윙
        if (coordDiff < 0 || coordDiff >= 2) {
            return strike(finalCoordinateNumber, pitchTiming, batterTiming, false);
        }

        int timingDiff = Math.abs(pitchTiming.ordinal() - batterTiming.ordinal());
        // 타이밍이 2칸 이상 어긋나면 헛스윙
        if (timingDiff >= 2) {
            return strike(finalCoordinateNumber, pitchTiming, batterTiming, false);
        }

        int diceCount = (coordDiff == 0 && timingDiff == 0) ? 2 : 1;
        int sumPenalty = 0;
        // 좌표·타이밍이 모두 1칸씩 어긋난 약한 접촉
        if (coordDiff == 1 && timingDiff == 1) {
            sumPenalty += SOFT_CONTACT_PENALTY;
        }
        // 볼존 타격 페널티 (중첩 가능)
        if (!finalCoordinateIsStrike) {
            sumPenalty += SOFT_CONTACT_PENALTY;
        }

        return withDice(finalCoordinateNumber, pitchTiming, batterTiming, diceCount, sumPenalty);
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
                timedOut,
                0);
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
                timedOut,
                0);
    }

    // 타격 이벤트 시 호출되는 주사위 메서드
    private TurnJudgmentResult withDice(int finalCoordinateNumber,
                                        Timing pitchTiming,
                                        Timing batterTiming,
                                        int diceCount,
                                        int sumPenalty) {
        List<Integer> dice = rollDice(diceCount);
        return new TurnJudgmentResult(
                null,
                dice,
                batterTiming,
                finalCoordinateNumber,
                pitchTiming,
                false,
                sumPenalty);
    }

    /**
     * 5×5 격자에서 상하좌우(맨해튼) 거리.
     * 유효하지 않은 좌표(0 또는 1~25 밖)면 -1.
     */
    private int orthogonalDistance(int a, int b) {
        if (!isValidGridCoordinate(a) || !isValidGridCoordinate(b)) {
            return -1;
        }
        int ay = (a - 1) / GRID_SIZE;
        int ax = (a - 1) % GRID_SIZE;
        int by = (b - 1) / GRID_SIZE;
        int bx = (b - 1) % GRID_SIZE;
        return Math.abs(ay - by) + Math.abs(ax - bx);
    }

    private boolean isValidGridCoordinate(int coordinateNumber) {
        return coordinateNumber >= 1 && coordinateNumber <= GRID_SIZE * GRID_SIZE;
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
