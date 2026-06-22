package com.project.bluffball.domain.game.service.usecase.judgment;

import com.project.bluffball.domain.game.enums.TurnResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 주사위 눈금 합과 블러핑 숫자 대조 순수 계산 컴포넌트 (DB·Redis 접근 없음).
 *
 * <h3>겹침(동일 합) 소멸 규칙</h3>
 * <ul>
 *   <li>3루타 + 아웃 → 아웃 (3루타 소멸)</li>
 *   <li>3루타 + 병살 → 아웃 (병살·3루타 소멸)</li>
 *   <li>홈런 + 아웃/병살 → 홈런 (아웃·병살 소멸)</li>
 * </ul>
 *
 * <h3>소멸 적용 후 우선순위</h3>
 * 병살(주자 있을 때) → 아웃 → 홈런 → 3루타
 *
 * <h3>특수 번호 미매칭 시</h3>
 * <ul>
 *   <li>주사위 2개 + 두 눈금 동일 (더블) → 2루타</li>
 *   <li>그 외 → 1루타</li>
 * </ul>
 */
@Component
public class BluffingJudgmentCalculator {

    /**
     * 주사위 눈금 합을 블러핑 숫자와 대조해 최종 {@link TurnResult}를 확정한다.
     *
     * @param diceResults        각 주사위 눈금(1~6) 목록
     * @param outNumbers         투수 아웃 유발 번호
     * @param dpNumbers          투수 병살 유발 번호
     * @param tripleNumbers      타자 3루타 유발 번호
     * @param hrNumbers          타자 홈런 유발 번호
     * @param hasRunnersOnBase   루상 주자 존재 여부 (병살 판정용)
     */
    public TurnResult judge(List<Integer> diceResults,
                            List<Integer> outNumbers,
                            List<Integer> dpNumbers,
                            List<Integer> tripleNumbers,
                            List<Integer> hrNumbers,
                            boolean hasRunnersOnBase) {
        int sum = diceResults.stream().mapToInt(Integer::intValue).sum();

        boolean matchOut = contains(outNumbers, sum);
        boolean rawMatchDp = contains(dpNumbers, sum);
        boolean matchDp = hasRunnersOnBase && rawMatchDp;
        boolean matchTriple = contains(tripleNumbers, sum);
        boolean matchHr = contains(hrNumbers, sum);

        if (matchHr && (matchOut || matchDp)) {
            matchOut = false;
            matchDp = false;
        }
        if (matchTriple && rawMatchDp) {
            matchDp = false;
            matchOut = true;
            matchTriple = false;
        } else if (matchTriple && matchOut) {
            matchTriple = false;
        }

        if (matchDp) {
            return TurnResult.DOUBLE_PLAY;
        }
        if (matchOut) {
            return TurnResult.OUT;
        }
        if (matchHr) {
            return TurnResult.HOMERUN;
        }
        if (matchTriple) {
            return TurnResult.TRIPLE;
        }
        if (isDoubleDice(diceResults)) {
            return TurnResult.DOUBLE;
        }
        return TurnResult.SINGLE;
    }

    /** 주사위 2개이며 두 눈금이 동일한지 (1,1)~(6,6) */
    private boolean isDoubleDice(List<Integer> diceResults) {
        return diceResults.size() == 2 && diceResults.get(0).equals(diceResults.get(1));
    }

    private boolean contains(List<Integer> numbers, int sum) {
        return numbers != null && numbers.contains(sum);
    }
}
