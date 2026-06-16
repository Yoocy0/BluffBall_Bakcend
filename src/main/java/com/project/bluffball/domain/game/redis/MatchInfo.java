package com.project.bluffball.domain.game.redis;

import com.project.bluffball.domain.game.enums.GameStatus;
import com.project.bluffball.domain.user.record.enums.GameMode;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis 저장용 매치 설정 및 진행 정보 객체.
 * JPA 엔터티가 아니며, Spring Data Redis의 @RedisHash로 관리된다.
 *
 * <p>경기가 생성되는 순간부터 종료될 때까지 유지되며,
 * 서비스 레이어의 플레이어 검증, 타순 관리, 교체 룰 적용의 기준이 된다.
 * 경기 종료 시 수동으로 삭제한다.</p>
 *
 * <p>Redis 저장 구조</p>
 * <pre>
 * KEY   : MatchInfo:{matchSessionId}
 * FIELD : gameMode, matchStatus, pitcherUserId, batterLineup,
 *         currentBatterIndex, pitcherCardHand, usedAsPitcherIds
 * </pre>
 */
@RedisHash(value = "MatchInfo", timeToLive = 7200)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchInfo {

    /** Redis Key — matchSessionId (GameState, TurnResultSession과 동일한 ID 공유) */
    @Id
    private String id;

    /** 게임 모드 — Redis에 ordinal 정수로 저장 */
    @Enumerated(EnumType.ORDINAL)
    private GameMode gameMode;

    /** 매치 진행 상태 — Redis에 ordinal 정수로 저장 */
    @Enumerated(EnumType.ORDINAL)
    private GameStatus matchStatus;

    /** 현재 등판 중인 투수 유저 ID */
    private Long pitcherUserId;

    /**
     * 타순 라인업 — 순서가 보장된 타자 userId 목록.
     * 싱글: 1명 / 클랜 미니: 3명 / 클랜 정규: 9명
     */
    private List<Long> batterLineup;

    /** 현재 타석 인덱스 (batterLineup 기준, 0부터 시작) */
    private int currentBatterIndex;

    /**
     * 투수의 현재 카드 패 — 경기 시작 또는 투수 교체 시 5장 추첨하여 세팅.
     * 카드 ID 목록.
     */
    private List<Long> pitcherCardHand;

    /**
     * 이 경기에서 투수로 등판했던 유저 ID 목록.
     * 교체 룰: 한 번 투수로 등판한 유저는 타자로 교체된 후 재등판 불가.
     */
    private List<Long> usedAsPitcherIds;

    @Builder
    public MatchInfo(String id, GameMode gameMode, Long pitcherUserId, List<Long> batterLineup) {
        this.id = id;
        this.gameMode = gameMode;
        this.matchStatus = GameStatus.WAITING;
        this.pitcherUserId = pitcherUserId;
        this.batterLineup = batterLineup != null ? batterLineup : new ArrayList<>();
        this.currentBatterIndex = 0;
        this.pitcherCardHand = new ArrayList<>();
        this.usedAsPitcherIds = new ArrayList<>();
    }
}
