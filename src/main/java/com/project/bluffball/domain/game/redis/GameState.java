package com.project.bluffball.domain.game.redis;

import com.project.bluffball.domain.game.dto.progress.GameProgressSituation;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

/**
 * Redis 저장용 인게임 실시간 상태 객체.
 * JPA 엔터티가 아니며, Spring Data Redis의 @RedisHash로 관리된다.
 *
 * <p>매 턴 종료 시 TurnResultSession의 turnResult를 서비스 레이어가 읽어
 * 이 객체에 반영(덮어쓰기)한다. 경기 종료 시 수동으로 삭제한다.</p>
 *
 * <p>Redis 저장 구조</p>
 * <pre>
 * KEY   : GameState:{matchSessionId}
 * FIELD : totalInnings, gameOver, currentInning, isTop, homeScore, awayScore,
 *         balls, strikes, outs,
 *         firstBase, secondBase, thirdBase, turnNumber
 * </pre>
 */
@RedisHash(value = "GameState", timeToLive = 7200)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameState {

    /** Redis Key — matchSessionId를 그대로 사용 (1:1 대응) */
    @Id
    private String id;

    /** 총 이닝 수 — {@link com.project.bluffball.domain.game.service.GameProgressService} 초기화 시 설정 (0 = 미초기화) */
    private int totalInnings;

    /** 경기 종료 여부 — {@link com.project.bluffball.domain.game.service.usecase.executor.GameProgressExecutor}에서 설정 */
    private boolean gameOver;

    /** 현재 이닝 수 */
    private int currentInning;

    /** 이닝 초/말 구분 (true: 초, false: 말) */
    private boolean isTop;

    /** 홈팀 점수 */
    private int homeScore;

    /** 어웨이팀 점수 */
    private int awayScore;

    /** 볼 카운트 (0~3) */
    private int balls;

    /** 스트라이크 카운트 (0~2) */
    private int strikes;

    /** 아웃 카운트 (0~2) */
    private int outs;

    /** 1루 주자 유무 */
    private boolean firstBase;

    /** 2루 주자 유무 */
    private boolean secondBase;

    /** 3루 주자 유무 */
    private boolean thirdBase;

    /** 현재 진행 중인 턴 번호 (1부터 시작) */
    private int turnNumber = 1;

    /** 스트라이크 1개 추가 (3스트라이크 시 아웃·카운트 리셋 — MVP 단순 처리) */
    public void addStrike() {
        this.strikes++;
        if (this.strikes >= 3) {
            this.strikes = 0;
            this.balls = 0;
            this.outs++;
        }
    }

    /** 다음 턴으로 진행 */
    public void advanceTurn() {
        this.turnNumber++;
    }

    /**
     * 경기 진행 초기화 — GameProgressExecutor에서만 호출한다.
     *
     * @param totalInnings 총 이닝 수
     */
    public void initializeProgress(int totalInnings) {
        if (totalInnings <= 0) {
            throw new IllegalArgumentException("totalInnings must be positive. value=" + totalInnings);
        }
        this.totalInnings = totalInnings;
        this.gameOver = false;
        this.currentInning = 1;
        this.isTop = true;
        this.homeScore = 0;
        this.awayScore = 0;
        this.balls = 0;
        this.strikes = 0;
        this.outs = 0;
        this.firstBase = false;
        this.secondBase = false;
        this.thirdBase = false;
        this.turnNumber = 1;
    }

    /** GameProgressExecutor에서만 호출한다. */
    void markGameOver() {
        this.gameOver = true;
    }

    /**
     * 계산된 경기 상황을 반영한다.
     * {@link com.project.bluffball.domain.game.service.usecase.executor.GameProgressExecutor}에서만 호출한다.
     */
    public void applyProgressSituation(GameProgressSituation situation, boolean gameOver) {
        this.currentInning = situation.currentInning();
        this.isTop = situation.isTop();
        this.homeScore = situation.homeScore();
        this.awayScore = situation.awayScore();
        this.balls = situation.balls();
        this.strikes = situation.strikes();
        this.outs = situation.outs();
        this.firstBase = situation.firstBase();
        this.secondBase = situation.secondBase();
        this.thirdBase = situation.thirdBase();
        this.gameOver = gameOver;
    }

    @Builder
    public GameState(String id, int totalInnings, boolean gameOver,
                     int currentInning, boolean isTop,
                     int homeScore, int awayScore,
                     int balls, int strikes, int outs,
                     boolean firstBase, boolean secondBase, boolean thirdBase,
                     int turnNumber) {
        this.id = id;
        this.totalInnings = totalInnings;
        this.gameOver = gameOver;
        this.currentInning = currentInning;
        this.isTop = isTop;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.balls = balls;
        this.strikes = strikes;
        this.outs = outs;
        this.firstBase = firstBase;
        this.secondBase = secondBase;
        this.thirdBase = thirdBase;
        this.turnNumber = turnNumber > 0 ? turnNumber : 1;
    }
}
