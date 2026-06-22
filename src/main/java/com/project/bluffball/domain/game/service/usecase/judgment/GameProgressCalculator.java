package com.project.bluffball.domain.game.service.usecase.judgment;

import com.project.bluffball.domain.game.dto.progress.GameProgressSituation;
import com.project.bluffball.domain.game.enums.TurnResult;
import org.springframework.stereotype.Component;

/**
 * {@link TurnResult}를 현재 경기 상황에 반영하는 순수 계산 컴포넌트 (DB·Redis 접근 없음).
 *
 * <ul>
 *   <li>볼 4개 → 볼넷, 카운트 리셋, 주자 1루 진출(포스)</li>
 *   <li>스트라이크 3개 → 아웃 +1, 카운트 리셋</li>
 *   <li>아웃 3개 → 이닝 종료(초/말 전환 또는 경기 종료)</li>
 *   <li>안타/2·3루타/홈런 → 주자·타자 진루(타격 결과만큼), 홈 도달 시 득점</li>
 *   <li>폭투 + 주자 있음 → 볼 +1, 주자 1베이스 진루</li>
 * </ul>
 */
@Component
public class GameProgressCalculator {

    private static final int MAX_BALLS_BEFORE_WALK = 4;
    private static final int MAX_STRIKES_BEFORE_OUT = 3;
    private static final int OUTS_PER_INNING = 3;

    public record Transition(GameProgressSituation after, boolean gameOver) {
    }

    public Transition apply(GameProgressSituation before, TurnResult turnResult) {
        MutableState state = MutableState.from(before);

        switch (turnResult) {
            case BALL -> addBall(state);
            case STRIKE -> addStrike(state);
            case WALK -> applyWalk(state);
            case STRIKE_OUT, OUT -> recordOut(state, 1);
            case DOUBLE_PLAY -> {
                if (state.firstBase) {
                    state.firstBase = false;
                }
                recordOut(state, 2);
            }
            case SINGLE -> advanceHit(state, 1);
            case DOUBLE -> advanceHit(state, 2);
            case TRIPLE -> advanceHit(state, 3);
            case HOMERUN -> advanceHit(state, 4);
            case WILD_PITCH -> applyWildPitch(state);
        }

        return new Transition(state.toSituation(), state.gameOver);
    }

    private void addBall(MutableState state) {
        state.balls++;
        if (state.balls >= MAX_BALLS_BEFORE_WALK) {
            applyWalk(state);
        }
    }

    private void addStrike(MutableState state) {
        state.strikes++;
        if (state.strikes >= MAX_STRIKES_BEFORE_OUT) {
            recordOut(state, 1);
        }
    }

    private void applyWalk(MutableState state) {
        resetCount(state);
        if (!state.firstBase) {
            state.firstBase = true;
            return;
        }
        if (!state.secondBase) {
            state.secondBase = true;
            return;
        }
        if (!state.thirdBase) {
            state.thirdBase = true;
            state.secondBase = true;
            state.firstBase = true;
            return;
        }
        addRun(state, 1);
        state.firstBase = true;
        state.secondBase = true;
        state.thirdBase = true;
    }

    private void applyWildPitch(MutableState state) {
        state.balls++;
        if (state.hasRunnersOnBase()) {
            advanceRunnersOnly(state, 1);
        }
        if (state.balls >= MAX_BALLS_BEFORE_WALK) {
            applyWalk(state);
        }
    }

    private void recordOut(MutableState state, int outCount) {
        resetCount(state);
        state.outs += outCount;
        if (state.outs >= OUTS_PER_INNING) {
            endHalfInning(state);
        }
    }

    private void advanceHit(MutableState state, int bases) {
        resetCount(state);
        if (bases >= 4) {
            addRun(state, countRunners(state) + 1);
            clearBases(state);
            return;
        }

        boolean[] next = new boolean[4];
        int runs = 0;

        if (state.firstBase) {
            runs += moveRunner(1, bases, next);
        }
        if (state.secondBase) {
            runs += moveRunner(2, bases, next);
        }
        if (state.thirdBase) {
            runs += moveRunner(3, bases, next);
        }
        placeBatter(bases, next);

        state.firstBase = next[1];
        state.secondBase = next[2];
        state.thirdBase = next[3];
        addRun(state, runs);
    }

    private void advanceRunnersOnly(MutableState state, int bases) {
        boolean[] next = new boolean[4];
        int runs = 0;

        if (state.firstBase) {
            runs += moveRunner(1, bases, next);
        }
        if (state.secondBase) {
            runs += moveRunner(2, bases, next);
        }
        if (state.thirdBase) {
            runs += moveRunner(3, bases, next);
        }

        state.firstBase = next[1];
        state.secondBase = next[2];
        state.thirdBase = next[3];
        addRun(state, runs);
    }

    /** 주자를 {@code bases}만큼 진루시키고, 도착 베이스 배열({@code next})에 반영한다. */
    private int moveRunner(int fromBase, int bases, boolean[] next) {
        int destination = fromBase + bases;
        if (destination >= 4) {
            return 1;
        }
        next[destination] = true;
        return 0;
    }

    private void placeBatter(int bases, boolean[] next) {
        if (bases >= 1 && bases <= 3) {
            next[bases] = true;
        }
    }

    private void endHalfInning(MutableState state) {
        resetCount(state);
        clearBases(state);
        state.outs = 0;

        if (state.isTop) {
            state.isTop = false;
            return;
        }

        if (state.currentInning >= state.totalInnings) {
            state.gameOver = true;
            return;
        }

        state.isTop = true;
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

    private int countRunners(MutableState state) {
        int count = 0;
        if (state.firstBase) count++;
        if (state.secondBase) count++;
        if (state.thirdBase) count++;
        return count;
    }

    private void addRun(MutableState state, int runs) {
        if (runs <= 0) {
            return;
        }
        if (state.isTop) {
            state.awayScore += runs;
        } else {
            state.homeScore += runs;
        }
    }

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
