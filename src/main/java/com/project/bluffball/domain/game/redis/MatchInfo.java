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
 * <h3>블러핑 숫자 저장 구조</h3>
 * <p>모든 블러핑 숫자는 {@code Map<Long, List<Integer>>} (userId → 숫자 목록) 형태로 저장한다.</p>
 * <ul>
 *   <li>List 구조: 투수 교체·스킬 등으로 숫자 개수가 달라지는 경우를 수용한다.</li>
 *   <li>Map 구조: 싱글(1명) / 클랜전(다수 타자) 확장 시 수정 없이 동작한다.</li>
 * </ul>
 */
@RedisHash(value = "MatchInfo", timeToLive = 7200)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchInfo {

    @Id
    private String id;

    @Enumerated(EnumType.ORDINAL)
    private GameMode gameMode;

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
     * 싱글: 3장 / 팀전: 5장 / 투수 교체 등판: 4장
     */
    private List<Long> pitcherCardHand;

    /** 멀리건(카드 교체) 완료 여부 — 경기 당 1회만 허용. */
    private boolean mulliganDone;

    /**
     * 이 경기에서 투수로 등판했던 유저 ID 목록.
     * 교체 룰: 한 번 등판한 투수는 재등판 불가.
     */
    private List<Long> usedAsPitcherIds;

    // ── 블러핑 고유 숫자 (userId → 숫자 목록) ────────────────────────────────────

    /**
     * 투수별 아웃 유발 번호 목록 — userId → outNumList.
     * 눈금 합이 목록 중 하나와 일치하면 '아웃' 판정.
     */
    private Map<Long, List<Integer>> outNumbers;

    /**
     * 투수별 병살 유발 번호 목록 — userId → dpNumList.
     * 주자가 있을 때 눈금 합이 일치하면 '병살타' 판정.
     */
    private Map<Long, List<Integer>> dpNumbers;

    /**
     * 타자별 3루타 유발 번호 목록 — userId → tripleNumList.
     * 눈금 합이 일치하면 '3루타' 판정.
     */
    private Map<Long, List<Integer>> tripleNumbers;

    /**
     * 타자별 홈런 유발 번호 목록 — userId → hrNumList.
     * 눈금 합이 일치하면 '홈런' 판정.
     */
    private Map<Long, List<Integer>> hrNumbers;

    /** 멀리건 완료 처리 — MulliganExecutor에서만 호출한다. */
    public void completeMulligan() {
        this.mulliganDone = true;
    }

    @Builder
    public MatchInfo(String id, GameMode gameMode, Long pitcherUserId, List<Long> batterLineup) {
        this.id = id;
        this.gameMode = gameMode;
        this.matchStatus = GameStatus.WAITING;
        this.pitcherUserId = pitcherUserId;
        this.batterLineup = batterLineup != null ? batterLineup : new ArrayList<>();
        this.currentBatterIndex = 0;
        this.pitcherCardHand = new ArrayList<>();
        this.mulliganDone = false;
        this.usedAsPitcherIds = new ArrayList<>();
        this.outNumbers = new HashMap<>();
        this.dpNumbers = new HashMap<>();
        this.tripleNumbers = new HashMap<>();
        this.hrNumbers = new HashMap<>();
    }
}
