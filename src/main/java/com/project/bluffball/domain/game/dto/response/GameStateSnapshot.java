package com.project.bluffball.domain.game.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GameState 스코어보드 스냅샷 — Service 이벤트 조립용 DTO.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameStateSnapshot {

    private int inning;
    private boolean isTop;
    private int homeScore;
    private int awayScore;
    private int balls;
    private int strikes;
    private int outs;
    private boolean firstBase;
    private boolean secondBase;
    private boolean thirdBase;

    @Builder
    public GameStateSnapshot(int inning, boolean isTop,
                             int homeScore, int awayScore,
                             int balls, int strikes, int outs,
                             boolean firstBase, boolean secondBase, boolean thirdBase) {
        this.inning = inning;
        this.isTop = isTop;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.balls = balls;
        this.strikes = strikes;
        this.outs = outs;
        this.firstBase = firstBase;
        this.secondBase = secondBase;
        this.thirdBase = thirdBase;
    }
}
