package com.project.bluffball.domain.game.redis;



import com.project.bluffball.domain.game.enums.MatchQueueState;

import com.project.bluffball.domain.user.record.enums.GameMode;

import jakarta.persistence.EnumType;

import jakarta.persistence.Enumerated;

import lombok.AccessLevel;

import lombok.Builder;

import lombok.Getter;

import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;

import org.springframework.data.redis.core.RedisHash;



/**

 * Redis 저장용 매칭 큐 등록 정보.

 *

 * <p>유저별 큐 상태(WAITING / MATCHED)와 매칭 성사 시 sessionId를 추적한다.

 * TTL 600초 — 비정상 이탈 시 자동 정리.</p>

 */

@RedisHash(value = "MatchQueueEntry", timeToLive = 600)

@Getter

@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class MatchQueueEntry {



    /** Redis Key — 유저 ID */

    @Id

    private Long userId;



    /** 매칭 대상 게임 모드 (큐·규칙 구분용) */

    @Enumerated(EnumType.ORDINAL)

    private GameMode gameMode;



    /** 현재 큐 등록 상태 */

    @Enumerated(EnumType.ORDINAL)

    private MatchQueueState state;



    /** 매칭 성사 시 설정 — {@link MatchQueueState#MATCHED}일 때만 유효 */

    private String matchSessionId;



    @Builder

    public MatchQueueEntry(Long userId, GameMode gameMode, MatchQueueState state, String matchSessionId) {

        this.userId = userId;

        this.gameMode = gameMode;

        this.state = state;

        this.matchSessionId = matchSessionId;

    }



    /**

     * WAITING 상태 큐 등록 Entity를 생성한다.

     *

     * @param userId   등록할 유저 ID

     * @param gameMode 매칭 게임 모드

     */

    public static MatchQueueEntry waiting(Long userId, GameMode gameMode) {

        return MatchQueueEntry.builder()

                .userId(userId)

                .gameMode(gameMode)

                .state(MatchQueueState.WAITING)

                .build();

    }



    /**

     * 매칭 성사 상태로 전환한다.

     *

     * @param matchSessionId 생성된 매치 세션 ID

     */

    public void markMatched(String matchSessionId) {

        this.state = MatchQueueState.MATCHED;

        this.matchSessionId = matchSessionId;

    }

}


