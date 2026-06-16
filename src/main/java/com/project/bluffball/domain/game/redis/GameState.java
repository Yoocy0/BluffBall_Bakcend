package com.project.bluffball.domain.game.redis;

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
 * FIELD : currentInning, isTop, homeScore, awayScore,
 *         balls, strikes, outs,
 *         firstBase, secondBase, thirdBase
 * </pre>
 */
@RedisHash(value = "GameState", timeToLive = 7200)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameState {

    /** Redis Key — matchSessionId를 그대로 사용 (1:1 대응) */
    @Id
    private String id;

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

    @Builder
    public GameState(String id, int currentInning, boolean isTop,
                     int homeScore, int awayScore,
                     int balls, int strikes, int outs,
                     boolean firstBase, boolean secondBase, boolean thirdBase) {
        this.id = id;
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
    }
}
