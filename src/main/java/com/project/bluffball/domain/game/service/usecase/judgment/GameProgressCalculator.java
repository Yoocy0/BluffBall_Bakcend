package com.project.bluffball.domain.game.service.usecase.judgment;

import com.project.bluffball.domain.game.dto.progress.GameProgressSituation;
import com.project.bluffball.domain.game.enums.TurnResult;
import org.springframework.stereotype.Component;

/**
 * {@link TurnResult} → 경기 상태(카운트·주자·점수·이닝) 순수 계산.
 * DB·Redis 접근 없음. {@link com.project.bluffball.domain.game.service.GameProgressService#applyTurnResult}에서 호출.
 *
 * <h3>종료·연장 규칙</h3>
 * <ul>
 *   <li>규정 이닝(및 이후) 말 공격에서 홈이 동점·열세였다가 앞서면 즉시 종료(끝내기)</li>
 *   <li>규정 이닝 말 종료 시 동점이면 다음 이닝 초로 연장</li>
 *   <li>규정 이닝 말 종료 시 점수 차가 있으면 경기 종료</li>
 * </ul>
 */
@Component
public class GameProgressCalculator {

    /** 홈 = 4번째 베이스. fromBase + bases >= 4 이면 득점 */
    private static final int BASES_TO_HOME = 4;
    private static final int MAX_BALLS_BEFORE_WALK = 4;
    private static final int MAX_STRIKES_BEFORE_OUT = 3;
    private static final int OUTS_PER_INNING = 3;

    public record Transition(GameProgressSituation after, boolean gameOver) {
    }

    /**
     * 현재 아웃 수·카운트에 따라 적용할 판정을 보정한다.
     *
     * <ul>
     *   <li>2아웃에서는 병살 → 일반 아웃</li>
     *   <li>2S에서 STRIKE → STRIKE_OUT (타석 종료·타순 전진용)</li>
     *   <li>3B에서 BALL/WILD_PITCH → WALK</li>
     * </ul>
     */
    public TurnResult resolveEffectiveTurnResult(TurnResult turnResult, GameProgressSituation before) {
        if (turnResult == null) {
            return null;
        }
        if (turnResult == TurnResult.DOUBLE_PLAY && before.outs() >= OUTS_PER_INNING - 1) {
            return TurnResult.OUT;
        }
        if (turnResult == TurnResult.STRIKE && before.strikes() >= MAX_STRIKES_BEFORE_OUT - 1) {
            return TurnResult.STRIKE_OUT;
        }
        if ((turnResult == TurnResult.BALL || turnResult == TurnResult.WILD_PITCH)
                && before.balls() >= MAX_BALLS_BEFORE_WALK - 1) {
            return TurnResult.WALK;
        }
        return turnResult;
    }

    public Transition apply(GameProgressSituation before, TurnResult turnResult) {
        TurnResult effective = resolveEffectiveTurnResult(turnResult, before);
        MutableState state = MutableState.from(before);

        switch (effective) {
            case STRIKE -> applyStrike(state);           // strike++, 3이면 삼진
            case FOUL -> applyFoul(state);               // strike++ (2S에서는 유지)
            case BALL -> applyBall(state);               // ball++, 4이면 볼넷
            case WALK -> applyWalk(state);               // 타자·주자 1베이스 진루
            case STRIKE_OUT -> recordOut(state, 1);      // 삼진 아웃
            case OUT -> recordOut(state, 1);             // 일반 아웃
            case DOUBLE_PLAY -> applyDoublePlay(state);  // out 2 + 선행 주자(3→2→1) 아웃
            case SINGLE -> advanceBases(state, 1);         // 1베이스 진루
            case DOUBLE -> advanceBases(state, 2);         // 2베이스 진루
            case TRIPLE -> advanceBases(state, 3);         // 3베이스 진루
            case HOMERUN -> advanceBases(state, 4);        // 전원 홈 → 득점
            case WILD_PITCH -> applyWildPitch(state);    // ball++ + 주자 1베이스
        }

        return new Transition(state.toSituation(), state.gameOver);
    }

    /** strike++ — 3스트라이크면 삼진(out++, 카운트 리셋) */
    private void applyStrike(MutableState state) {
        state.strikes++;
        if (state.strikes >= MAX_STRIKES_BEFORE_OUT) {
            recordOut(state, 1);
        }
    }

    /** 파울 — strike++이지만 이미 2S면 카운트 유지(커트) */
    private void applyFoul(MutableState state) {
        if (state.strikes < MAX_STRIKES_BEFORE_OUT - 1) {
            state.strikes++;
        }
    }

    /** ball++ — 4볼이면 볼넷(타자·주자 1베이스 진루) */
    private void applyBall(MutableState state) {
        state.balls++;
        if (state.balls >= MAX_BALLS_BEFORE_WALK) {
            applyWalk(state);
        }
    }

    /** 볼넷 — single과 동일: 타자·주자 모두 1베이스 진루 */
    private void applyWalk(MutableState state) {
        advanceBases(state, 1);
    }

    /** out++ — 3아웃이면 이닝 종료(초/말 전환 또는 경기 종료) */
    private void recordOut(MutableState state, int outCount) {
        resetCount(state);
        state.outs += outCount;
        if (state.outs >= OUTS_PER_INNING) {
            endHalfInning(state);
        }
    }

    /**
     * 병살 — out 2개.
     * 주자가 있으면 선행 주자(3→2→1루 순, 가장 앞선 주자) 제거 + 타자 아웃.
     */
    private void applyDoublePlay(MutableState state) {
        if (state.hasRunnersOnBase()) {
            removeLeadRunner(state);
        }
        recordOut(state, 2);
    }

    /**
     * 타격·볼넷 진루 — 타자·주자 모두 {@code bases}만큼 이동.
     * 홈(4) 도달 시 주자 1명당 1점. 홈런(bases=4)은 타자 포함 전원 득점.
     */
    private void advanceBases(MutableState state, int bases) {
        resetCount(state);

        // 홈런: 루상 주자 + 타자 전원 홈
        if (bases >= BASES_TO_HOME) {
            addRun(state, countRunners(state) + 1);
            clearBases(state);
            return;
        }

        // next[1~3] = 진루 후 1·2·3루 점유 여부 (계산용 임시 배열)
        boolean[] next = emptyBases();
        int runs = 0;

        // 기존 주자 각각 bases만큼 이동 (홈 도달 시 runs++)
        if (state.firstBase) {
            runs += moveRunner(1, bases, next);
        }
        if (state.secondBase) {
            runs += moveRunner(2, bases, next);
        }
        if (state.thirdBase) {
            runs += moveRunner(3, bases, next);
        }
        // 타자 진루 — bases가 1~3이면 해당 베이스에 배치
        placeBatter(bases, next);

        applyBases(state, next);
        addRun(state, runs);
    }

    /**
     * 폭투 — ball++.
     * 4볼이면 볼넷만 적용(주자 이중 진루 방지).
     * 그 외 주자 있으면 모든 주자 1베이스 진루(타자 제외).
     */
    private void applyWildPitch(MutableState state) {
        state.balls++;
        // 4볼 → 볼넷 진루로 일괄 처리 (WP 주자 진루 + walk 중복 방지)
        if (state.balls >= MAX_BALLS_BEFORE_WALK) {
            applyWalk(state);
            return;
        }
        if (state.hasRunnersOnBase()) {
            boolean[] next = emptyBases();
            int runs = 0;

            if (state.firstBase) {
                runs += moveRunner(1, 1, next);
            }
            if (state.secondBase) {
                runs += moveRunner(2, 1, next);
            }
            if (state.thirdBase) {
                runs += moveRunner(3, 1, next);
            }

            applyBases(state, next);
            addRun(state, runs);
        }
    }

    /** 병살 시 선행 주자(가장 앞선 주자) 제거 — 3루 → 2루 → 1루 순 */
    private void removeLeadRunner(MutableState state) {
        if (state.thirdBase) {
            state.thirdBase = false;
            return;
        }
        if (state.secondBase) {
            state.secondBase = false;
            return;
        }
        state.firstBase = false;
    }

    /**
     * 주자 1명을 {@code bases}만큼 진루.
     * @return 홈 도달 시 1(득점), 아니면 0
     */
    private int moveRunner(int fromBase, int bases, boolean[] next) {
        int destination = fromBase + bases;
        if (destination >= BASES_TO_HOME) {
            return 1;
        }
        next[destination] = true;
        return 0;
    }

    /** 타자를 {@code bases}만큼 진루한 뒤 도착 베이스(1~3)에 배치 */
    private void placeBatter(int bases, boolean[] next) {
        if (bases >= 1 && bases <= 3) {
            next[bases] = true;
        }
    }

    /**
     * 3아웃 — 카운트·주자 리셋 후 초→말, 말→다음 이닝 또는 경기 종료.
     *
     * <p>규정 이닝(및 연장) 말 종료 시 동점이면 {@code totalInnings}는 유지한 채
     * {@code currentInning}만 올려 연장 초로 진행한다.</p>
     */
    private void endHalfInning(MutableState state) {
        resetCount(state);
        clearBases(state);
        state.outs = 0;

        if (state.isTop) {
            state.isTop = false; // 초 종료 → 말
            return;
        }

        if (state.currentInning >= state.totalInnings) {
            if (state.homeScore == state.awayScore) {
                // 동점 → 연장 (다음 이닝 초). totalInnings는 규정 이닝 길이로 유지
                state.isTop = true;
                state.currentInning++;
                return;
            }
            state.gameOver = true; // 점수 차 확정 → 경기 종료
            return;
        }

        state.isTop = true; // 말 종료 → 다음 이닝 초
        state.currentInning++;
    }

    private void resetCount(MutableState state) {
        state.balls = 0;
        state.strikes = 0;
    }

    private void clearBases(MutableState state) {
        state.firstBase = false;
        state.secondBase = false;
        state.thirdBase = false;
    }

    /** next[0] 미사용, next[1~3] = 1·2·3루 */
    private boolean[] emptyBases() {
        return new boolean[4];
    }

    private void applyBases(MutableState state, boolean[] next) {
        state.firstBase = next[1];
        state.secondBase = next[2];
        state.thirdBase = next[3];
    }

    private int countRunners(MutableState state) {
        int count = 0;
        if (state.firstBase) count++;
        if (state.secondBase) count++;
        if (state.thirdBase) count++;
        return count;
    }

    /**
     * 초(isTop)=어웨이 득점, 말=홈 득점.
     *
     * <p>규정 이닝 이상 말 공격에서 홈이 동점·열세였다가 앞서면 끝내기로 즉시 종료한다.</p>
     */
    private void addRun(MutableState state, int runs) {
        if (runs <= 0) {
            return;
        }
        if (state.isTop) {
            state.awayScore += runs;
            return;
        }

        boolean wasNotLeading = state.homeScore <= state.awayScore;
        state.homeScore += runs;
        if (wasNotLeading
                && state.homeScore > state.awayScore
                && state.currentInning >= state.totalInnings) {
            state.gameOver = true;
        }
    }

    /** 계산 중 가변 상태 — apply() 종료 시 GameProgressSituation으로 변환 */
    private static final class MutableState {
        int totalInnings;
        int currentInning;
        boolean isTop;
        int homeScore;
        int awayScore;
        int balls;
        int strikes;
        int outs;
        boolean firstBase;
        boolean secondBase;
        boolean thirdBase;
        boolean gameOver;

        static MutableState from(GameProgressSituation situation) {
            MutableState state = new MutableState();
            state.totalInnings = situation.totalInnings();
            state.currentInning = situation.currentInning();
            state.isTop = situation.isTop();
            state.homeScore = situation.homeScore();
            state.awayScore = situation.awayScore();
            state.balls = situation.balls();
            state.strikes = situation.strikes();
            state.outs = situation.outs();
            state.firstBase = situation.firstBase();
            state.secondBase = situation.secondBase();
            state.thirdBase = situation.thirdBase();
            return state;
        }

        boolean hasRunnersOnBase() {
            return firstBase || secondBase || thirdBase;
        }

        GameProgressSituation toSituation() {
            return new GameProgressSituation(
                    totalInnings, currentInning, isTop,
                    homeScore, awayScore,
                    balls, strikes, outs,
                    firstBase, secondBase, thirdBase);
        }
    }
}
