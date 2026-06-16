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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 투수의 현재 카드 패 — 카드 ID 목록.
     * 드로우 장수는 게임 모드와 상황에 따라 결정된다.
     * 싱글: 3장 / 팀전: 5장 / 투수 교체 등판: 4장
     */
    private List<Long> pitcherCardHand;

    /**
     * 이 경기에서 투수로 등판했던 유저 ID 목록.
     * 교체 룰: 한 번 투수로 등판한 유저는 타자로 교체된 후 재등판 불가.
     */
    private List<Long> usedAsPitcherIds;

    // ── 블러핑 고유 숫자 (게임 시작 시 제출, 경기 전체에 적용) ──────────────────

    /**
     * 투수가 지정한 아웃 유발 번호 목록 (5개).
     * 주사위 눈금이 이 중 하나와 일치하면 '아웃' 판정.
     */
    private List<Integer> pitcherOutNumbers;

    /**
     * 투수가 지정한 병살 유발 번호 (1개).
     * 주자가 있는 상황에서 주사위 눈금이 일치하면 '병살타' 판정.
     */
    private Integer pitcherDoublePlayNumber;

    /**
     * 타자별 3루타 번호 — userId → 3루타 지정 번호.
     * 주사위 눈금이 해당 타자의 번호와 일치하면 '3루타' 판정.
     * 싱글: 1개 항목 / 클랜 미니: 3개 항목 / 클랜 정규: 9개 항목
     */
    private Map<Long, Integer> batterTripleNumbers;

    /**
     * 타자별 홈런 번호 — userId → 홈런 지정 번호.
     * 주사위 눈금이 해당 타자의 번호와 일치하면 '홈런' 판정.
     * 싱글: 1개 항목 / 클랜 미니: 3개 항목 / 클랜 정규: 9개 항목
     */
    private Map<Long, Integer> batterHomerunNumbers;

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
        this.batterTripleNumbers = new HashMap<>();
        this.batterHomerunNumbers = new HashMap<>();
    }
}
