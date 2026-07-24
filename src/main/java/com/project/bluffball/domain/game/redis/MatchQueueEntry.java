package com.project.bluffball.domain.game.redis;

import com.project.bluffball.domain.game.enums.MatchQueueState;
import com.project.bluffball.domain.league.enums.LeagueTier;
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
 * 리그 매칭 시 {@code leagueTier}·{@code teamId}를 함께 저장한다.
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

    /** 리그 티어 — 쇼다운은 null */
    @Enumerated(EnumType.ORDINAL)
    private LeagueTier leagueTier;

    /** 리그 매칭 팀 ID — 쇼다운은 null */
    private Long teamId;

    /** 현재 큐 등록 상태 */
    @Enumerated(EnumType.ORDINAL)
    private MatchQueueState state;

    /** 매칭 성사 시 설정 — {@link MatchQueueState#MATCHED}일 때만 유효 */
    private String matchSessionId;

    @Builder
    public MatchQueueEntry(
            Long userId,
            GameMode gameMode,
            LeagueTier leagueTier,
            Long teamId,
            MatchQueueState state,
            String matchSessionId) {
        this.userId = userId;
        this.gameMode = gameMode;
        this.leagueTier = leagueTier;
        this.teamId = teamId;
        this.state = state;
        this.matchSessionId = matchSessionId;
    }

    /**
     * 쇼다운 WAITING 큐 등록을 생성한다.
     *
     * @param userId 등록할 유저 ID
     * @param gameMode 매칭 게임 모드
     * @return WAITING Entry
     */
    public static MatchQueueEntry waiting(Long userId, GameMode gameMode) {
        return MatchQueueEntry.builder()
                .userId(userId)
                .gameMode(gameMode)
                .state(MatchQueueState.WAITING)
                .build();
    }

    /**
     * 리그 WAITING 큐 등록을 생성한다.
     *
     * @param userId 등록할 유저 ID (리더)
     * @param gameMode COMPACT_LEAGUE / FULL_LEAGUE
     * @param leagueTier 리그 단계
     * @param teamId 팀 ID
     * @return WAITING Entry
     */
    public static MatchQueueEntry waitingLeague(
            Long userId,
            GameMode gameMode,
            LeagueTier leagueTier,
            Long teamId) {
        return MatchQueueEntry.builder()
                .userId(userId)
                .gameMode(gameMode)
                .leagueTier(leagueTier)
                .teamId(teamId)
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
