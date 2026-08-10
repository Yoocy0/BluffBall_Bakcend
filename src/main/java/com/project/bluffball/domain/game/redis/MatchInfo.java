package com.project.bluffball.domain.game.redis;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.domain.game.enums.GameStatus;
import com.project.bluffball.domain.game.enums.SetupKind;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.user.record.enums.GameMode;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis 저장용 매치 설정 및 진행 정보 객체.
 * JPA 엔터티가 아니며, Spring Data Redis의 @RedisHash로 관리된다.
 *
 * <p>경기가 생성되는 순간부터 종료될 때까지 유지되며,
 * 서비스 레이어의 플레이어 검증, 타순 관리, 교체 룰 적용의 기준이 된다.
 * 경기 종료 시 수동으로 삭제한다.</p>
 *
 * <h3>블러핑 숫자 저장 구조</h3>
 * <p>플레이어별 제출은 {@link PlayerSetupNumbers} 리스트로 저장한다.
 * Spring Data Redis는 {@code Map&lt;Long, List&lt;Integer&gt;&gt;} flatten 시
 * 리스트 요소를 저장하지 못하므로 List 구조를 사용한다.</p>
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

    /** 홈팀 유저 ID — 매치 생성 시 초기 투수로 고정 (공수 교대와 무관) */
    private Long homeUserId;

    /** 어웨이팀 유저 ID — 매치 생성 시 초기 타자로 고정 (공수 교대와 무관) */
    private Long awayUserId;

    /** 홈 팀 ID — 리그 매치에서 사용 */
    private Long homeTeamId;

    /** 어웨이 팀 ID — 리그 매치에서 사용 */
    private Long awayTeamId;

    /** 리그 경기 결과(점수·보상) 반영 완료 여부 */
    private boolean leagueResultApplied;

    /**
     * 연습용 봇 매치 여부.
     *
     * <p>true이면 보상·래더/순위 등 사이드이펙트를 건너뛴다.
     * (현재 Showdown PvP에도 보상 경로가 없으므로, 향후 보상 추가 시 이 플래그로 가드한다.)</p>
     */
    private boolean practiceBotMatch;

    /** 연습 봇 난이도 — practiceBotMatch일 때만 사용 */
    @Enumerated(EnumType.ORDINAL)
    private BotDifficulty botDifficulty;

    /** 연습 봇 유저 ID — practiceBotMatch일 때만 사용 */
    private Long botUserId;

    /**
     * 봇 드로우/멀리건 풀(인스턴스 ID).
     *
     * <p>사람 참가자는 기존처럼 전체 마스터 풀을 쓰고, 봇만 이 목록을 사용한다.</p>
     */
    private List<Long> botDrawPool;

    /** 리그 티어 — 리그 매치에서 사용 */
    @Enumerated(EnumType.ORDINAL)
    private LeagueTier leagueTier;

    /** 홈 출전 전원(타순 + Compact 전담 투수) — 리그 매치 */
    private List<Long> homeRosterUserIds;

    /** 어웨이 출전 전원 — 리그 매치 */
    private List<Long> awayRosterUserIds;

    /** 홈 타순 — Compact는 로스터와 다를 수 있음 */
    private List<Long> homeBattingOrderUserIds;

    /** 어웨이 타순 */
    private List<Long> awayBattingOrderUserIds;

    /** 홈 선발 투수 — 리그 매치 (공수 교대 시 기준) */
    private Long homeStartingPitcherUserId;

    /** 어웨이 선발 투수 — 리그 매치 (공수 교대 시 기준) */
    private Long awayStartingPitcherUserId;

    /** 홈 현재 등판 투수 — 교체 반영 (다음 수비 이닝에도 유지) */
    private Long homeActivePitcherUserId;

    /** 어웨이 현재 등판 투수 — 교체 반영 */
    private Long awayActivePitcherUserId;

    /** 홈 팀 다음 타석 인덱스 (공수 교대 후에도 이어감) */
    private int homeNextBatterIndex;

    /** 어웨이 팀 다음 타석 인덱스 */
    private int awayNextBatterIndex;

    /** 홈 팀 투수 교체 사용 횟수 */
    private int homePitcherSubstitutionCount;

    /** 어웨이 팀 투수 교체 사용 횟수 */
    private int awayPitcherSubstitutionCount;

    /** 현재 등판 중인 투수 유저 ID */
    private Long pitcherUserId;

    /**
     * 타순 라인업 — 순서가 보장된 타자 userId 목록.
     * 싱글: 1명 / Compact: 3명 / Full: 9명
     */
    private List<Long> batterLineup;

    /** 현재 타석 인덱스 (batterLineup 기준, 0부터 시작) */
    private int currentBatterIndex;

    /**
     * 투수의 현재 카드 패 — 카드 ID 목록.
     * 쇼다운: 3장 / Compact: 4장(교체 후 3) / Full: 5장(교체 후 4)
     */
    private List<Long> pitcherCardHand;

    /** 멀리건(카드 교체) 완료 여부 — 모든 참가자 완료 시 true (하위 호환). */
    private boolean mulliganDone;

    /** 플레이어별 구종 카드 패 — Redis List 구조 */
    private List<PlayerCardHand> playerCardHands;

    /** 멀리건을 완료한 참가자 userId 목록 */
    private List<Long> mulliganDoneUserIds;

    /**
     * 이 경기에서 투수로 등판했던 유저 ID 목록.
     * 교체 룰: 한 번 등판한 투수는 재등판 불가.
     */
    private List<Long> usedAsPitcherIds;

    /** 플레이어별 블러핑 숫자 — Redis에 List로 저장 */
    private List<PlayerSetupNumbers> playerSetupNumbers;

    /** 플레이어별 투수 교체 시 제외 카드 — 리그 사전 선택 dropCardId */
    private List<PlayerDropCard> playerDropCards;

    /**
     * 2루타 판정용 목표 주사위 눈금 (1~6).
     * 경기(매치) 생성 시 {@link #initializeDoubleJudgmentSettings()}로 랜덤 설정한다.
     */
    private int doubleJudgmentTargetFace;

    /**
     * 2루타 판정 시 비교할 주사위 위치.
     * {@code true} = 앞 주사위(diceResults[0]), {@code false} = 뒷 주사위(diceResults[1]).
     */
    private boolean doubleJudgmentUseFrontDice;

    /** 경기 시작 시 2루타 판정 규칙을 랜덤 생성한다. */
    public void initializeDoubleJudgmentSettings() {
        this.doubleJudgmentTargetFace = ThreadLocalRandom.current().nextInt(1, 7);
        this.doubleJudgmentUseFrontDice = ThreadLocalRandom.current().nextBoolean();
    }

    public boolean isDoubleJudgmentConfigured() {
        return doubleJudgmentTargetFace >= 1 && doubleJudgmentTargetFace <= 6;
    }

    /** Redis 역직렬화용 */
    void setDoubleJudgmentTargetFace(int doubleJudgmentTargetFace) {
        this.doubleJudgmentTargetFace = doubleJudgmentTargetFace;
    }

    /** Redis 역직렬화용 */
    void setDoubleJudgmentUseFrontDice(boolean doubleJudgmentUseFrontDice) {
        this.doubleJudgmentUseFrontDice = doubleJudgmentUseFrontDice;
    }

    /** 멀리건 완료 처리 — 모든 참가자 완료 시 호출 (하위 호환). */
    public void completeMulligan() {
        this.mulliganDone = true;
    }

    /** 특정 플레이어의 멀리건 완료 처리 */
    public void completeMulliganForUser(Long userId) {
        ensureCollectionsInitialized();
        if (!mulliganDoneUserIds.contains(userId)) {
            mulliganDoneUserIds.add(userId);
        }
        if (isAllMulliganDone(getParticipantUserIds())) {
            this.mulliganDone = true;
        }
    }

    public boolean isMulliganDoneForUser(Long userId) {
        ensureCollectionsInitialized();
        return mulliganDoneUserIds.contains(userId);
    }

    public boolean isAllMulliganDone(List<Long> participantUserIds) {
        ensureCollectionsInitialized();
        return !participantUserIds.isEmpty()
                && mulliganDoneUserIds.containsAll(participantUserIds);
    }

    /** 매치 참가자 userId (중복 제거, 순서 유지) */
    public List<Long> getParticipantUserIds() {
        ensureCollectionsInitialized();
        Set<Long> ids = new LinkedHashSet<>();
        // 리그: 양 팀 로스터 전원
        if (!homeRosterUserIds.isEmpty() || !awayRosterUserIds.isEmpty()) {
            ids.addAll(homeRosterUserIds);
            ids.addAll(awayRosterUserIds);
            return new ArrayList<>(ids);
        }
        // 쇼다운: 투수 + 타순
        if (pitcherUserId != null) {
            ids.add(pitcherUserId);
        }
        ids.addAll(batterLineup);
        return new ArrayList<>(ids);
    }

    public List<Long> getPlayerCardHand(Long userId) {
        ensureCollectionsInitialized();
        for (PlayerCardHand entry : playerCardHands) {
            if (userId.equals(entry.getUserId())) {
                return new ArrayList<>(entry.getCardIds());
            }
        }
        return new ArrayList<>();
    }

    public void setPlayerCardHand(Long userId, List<Long> cardIds) {
        ensureCollectionsInitialized();
        playerCardHands.removeIf(entry -> userId.equals(entry.getUserId()));
        playerCardHands.add(new PlayerCardHand(userId, cardIds));
        syncPitcherCardHandFromPlayer();
    }

    /** 현재 등판 투수의 패를 pitcherCardHand에 동기화 */
    public void syncPitcherCardHandFromPlayer() {
        ensureCollectionsInitialized();
        pitcherCardHand.clear();
        if (pitcherUserId != null) {
            pitcherCardHand.addAll(getPlayerCardHand(pitcherUserId));
        }
    }

    public void clearMulliganPhase() {
        ensureCollectionsInitialized();
        this.mulliganDone = false;
        mulliganDoneUserIds.clear();
        playerCardHands.clear();
        pitcherCardHand.clear();
    }

    /**
     * 공수 교대 시 멀리건·패를 초기화한다.
     * {@link com.project.bluffball.domain.game.service.usecase.executor.HalfInningRoleSwapExecutor} 전용.
     */
    public void resetForNewHalfInning() {
        clearMulliganPhase();
    }

    /**
     * 싱글 모드(1 vs 1) 공수 교대 — 투수·타자 userId를 교환한다.
     *
     * @return 새 등판 투수 userId
     */
    public Long swapRolesForSingleMode() {
        ensureCollectionsInitialized();
        if (batterLineup.isEmpty()) {
            throw new BadRequestException(ErrorCode.GAME_LINEUP_EMPTY);
        }

        Long previousPitcher = pitcherUserId;
        Long newPitcher = batterLineup.get(0);

        if (!usedAsPitcherIds.contains(previousPitcher)) {
            usedAsPitcherIds.add(previousPitcher);
        }

        pitcherUserId = newPitcher;
        batterLineup.set(0, previousPitcher);
        syncPitcherCardHandFromPlayer();

        return newPitcher;
    }

    /**
     * Redis 역직렬화 후 null이 될 수 있는 컬렉션 필드를 초기화한다.
     * Reader·Executor에서 조회 직후 호출한다.
     */
    public void ensureCollectionsInitialized() {
        if (batterLineup == null) {
            batterLineup = new ArrayList<>();
        }
        if (pitcherCardHand == null) {
            pitcherCardHand = new ArrayList<>();
        }
        if (usedAsPitcherIds == null) {
            usedAsPitcherIds = new ArrayList<>();
        }
        if (playerSetupNumbers == null) {
            playerSetupNumbers = new ArrayList<>();
        }
        if (playerCardHands == null) {
            playerCardHands = new ArrayList<>();
        }
        if (mulliganDoneUserIds == null) {
            mulliganDoneUserIds = new ArrayList<>();
        }
        if (homeRosterUserIds == null) {
            homeRosterUserIds = new ArrayList<>();
        }
        if (awayRosterUserIds == null) {
            awayRosterUserIds = new ArrayList<>();
        }
        if (homeBattingOrderUserIds == null) {
            homeBattingOrderUserIds = new ArrayList<>();
        }
        if (awayBattingOrderUserIds == null) {
            awayBattingOrderUserIds = new ArrayList<>();
        }
        if (playerDropCards == null) {
            playerDropCards = new ArrayList<>();
        }
        if (botDrawPool == null) {
            botDrawPool = new ArrayList<>();
        }
        for (PlayerSetupNumbers entry : playerSetupNumbers) {
            entry.ensureListsInitialized();
        }
        for (PlayerCardHand entry : playerCardHands) {
            entry.ensureListsInitialized();
        }
    }

    /**
     * SetupNumberExecutor 전용 — 종류별 병합 upsert.
     *
     * <p>{@link SetupKind#FULL}은 4종을 통째로 교체한다.
     * {@link SetupKind#PITCHER}/{@link SetupKind#BATTER}는 해당 필드만 갱신한다.</p>
     *
     * @param userId 유저 ID
     * @param setupKind 제출 종류
     * @param request 블러핑 숫자
     */
    public void upsertPlayerSetupNumbers(Long userId, SetupKind setupKind, SetupNumberRequest request) {
        ensureCollectionsInitialized();
        PlayerSetupNumbers existing = findPlayerSetupNumbers(userId);
        if (existing == null) {
            existing = new PlayerSetupNumbers(userId, List.of(), List.of(), List.of(), List.of());
            playerSetupNumbers.add(existing);
        }
        switch (setupKind) {
            case FULL -> {
                existing.setOutNumList(nullToEmpty(request.outNumList()));
                existing.setDpNumList(nullToEmpty(request.dpNumList()));
                existing.setTripleNumList(nullToEmpty(request.tripleNumList()));
                existing.setHrNumList(nullToEmpty(request.hrNumList()));
            }
            case PITCHER -> {
                existing.setOutNumList(nullToEmpty(request.outNumList()));
                existing.setDpNumList(nullToEmpty(request.dpNumList()));
            }
            case BATTER -> {
                existing.setTripleNumList(nullToEmpty(request.tripleNumList()));
                existing.setHrNumList(nullToEmpty(request.hrNumList()));
            }
        }
    }

    /**
     * null 리스트를 빈 리스트로 치환한다.
     *
     * @param list 입력
     * @return non-null 리스트
     */
    private static List<Integer> nullToEmpty(List<Integer> list) {
        return list != null ? list : List.of();
    }

    /**
     * 투수 셋업(OUT·병살)을 비운다 — 교체 등판 후 재제출용.
     *
     * @param userId 유저 ID
     */
    public void clearPitcherSetupNumbers(Long userId) {
        ensureCollectionsInitialized();
        PlayerSetupNumbers existing = findPlayerSetupNumbers(userId);
        if (existing != null) {
            existing.setOutNumList(List.of());
            existing.setDpNumList(List.of());
        }
    }

    /**
     * 타자 셋업(3루타·홈런)을 비운다 — 투수→타자 전환 후 재제출용.
     *
     * @param userId 유저 ID
     */
    public void clearBatterSetupNumbers(Long userId) {
        ensureCollectionsInitialized();
        PlayerSetupNumbers existing = findPlayerSetupNumbers(userId);
        if (existing != null) {
            existing.setTripleNumList(List.of());
            existing.setHrNumList(List.of());
        }
    }

    /**
     * 투수 셋업이 채워졌는지 반환한다.
     *
     * @param userId 유저 ID
     * @return OUT 5개·병살 1개이면 true
     */
    public boolean hasPitcherSetup(Long userId) {
        PlayerSetupNumbers existing = findPlayerSetupNumbers(userId);
        if (existing == null) {
            return false;
        }
        existing.ensureListsInitialized();
        return existing.getOutNumList().size() == 5 && existing.getDpNumList().size() == 1;
    }

    /**
     * 타자 셋업이 채워졌는지 반환한다.
     *
     * @param userId 유저 ID
     * @return 3루타 1개·홈런 1개이면 true
     */
    public boolean hasBatterSetup(Long userId) {
        PlayerSetupNumbers existing = findPlayerSetupNumbers(userId);
        if (existing == null) {
            return false;
        }
        existing.ensureListsInitialized();
        return existing.getTripleNumList().size() == 1 && existing.getHrNumList().size() == 1;
    }

    /**
     * 쇼다운 일괄 셋업이 채워졌는지 반환한다.
     *
     * @param userId 유저 ID
     * @return 4종 모두 채워졌으면 true
     */
    public boolean hasFullSetup(Long userId) {
        return hasPitcherSetup(userId) && hasBatterSetup(userId);
    }

    /**
     * dropCardId를 저장한다.
     *
     * @param userId 유저 ID
     * @param dropCardId 교체 시 제외 카드
     */
    public void setPlayerDropCard(Long userId, Long dropCardId) {
        ensureCollectionsInitialized();
        playerDropCards.removeIf(entry -> userId.equals(entry.getUserId()));
        if (dropCardId != null) {
            playerDropCards.add(new PlayerDropCard(userId, dropCardId));
        }
    }

    /**
     * 유저의 dropCardId를 반환한다.
     *
     * @param userId 유저 ID
     * @return dropCardId, 없으면 null
     */
    public Long getPlayerDropCard(Long userId) {
        ensureCollectionsInitialized();
        for (PlayerDropCard entry : playerDropCards) {
            if (userId.equals(entry.getUserId())) {
                return entry.getDropCardId();
            }
        }
        return null;
    }

    /**
     * 투수 교체 횟수를 수비 팀에 맞게 1 증가시킨다.
     *
     * @deprecated 팀별 카운터로 대체 — {@link #incrementDefendingPitcherSubstitutionCount()} 사용
     */
    @Deprecated
    public void incrementPitcherSubstitutionCount() {
        incrementDefendingPitcherSubstitutionCount();
    }

    /**
     * 현재 수비 팀의 투수 교체 횟수를 1 증가시킨다.
     */
    public void incrementDefendingPitcherSubstitutionCount() {
        if (isHomeDefending()) {
            this.homePitcherSubstitutionCount++;
        } else {
            this.awayPitcherSubstitutionCount++;
        }
    }

    /**
     * 현재 수비 팀의 투수 교체 횟수를 반환한다.
     *
     * @return 교체 횟수
     */
    public int getDefendingPitcherSubstitutionCount() {
        return isHomeDefending() ? homePitcherSubstitutionCount : awayPitcherSubstitutionCount;
    }

    /**
     * 홈이 수비(투수) 중인지 반환한다.
     *
     * @return 홈 수비면 true
     */
    public boolean isHomeDefending() {
        ensureCollectionsInitialized();
        if (pitcherUserId == null) {
            return true;
        }
        if (homeRosterUserIds.contains(pitcherUserId)) {
            return true;
        }
        if (awayRosterUserIds.contains(pitcherUserId)) {
            return false;
        }
        // 로스터 판별 불가 시 active 투수 기준
        return pitcherUserId.equals(homeActivePitcherUserId);
    }

    /**
     * 현재 등판 투수를 교체한다. 수비 팀 active 투수도 갱신한다.
     *
     * <p>타순 슬롯 교환은 {@link #swapDefendingBattingOrderOnPitcherSubstitute(Long, Long)} 에서 수행한다.</p>
     *
     * @param newPitcherUserId 신임 투수
     */
    public void substitutePitcher(Long newPitcherUserId) {
        ensureCollectionsInitialized();
        if (pitcherUserId != null && !usedAsPitcherIds.contains(pitcherUserId)) {
            usedAsPitcherIds.add(pitcherUserId);
        }
        boolean homeDefending = isHomeDefending();
        this.pitcherUserId = newPitcherUserId;
        if (homeDefending) {
            this.homeActivePitcherUserId = newPitcherUserId;
        } else {
            this.awayActivePitcherUserId = newPitcherUserId;
        }
        syncPitcherCardHandFromPlayer();
        incrementDefendingPitcherSubstitutionCount();
    }

    /**
     * 투수 교체 시 수비 팀 타순을 맞춘다.
     *
     * <ul>
     *   <li>신임 투수가 타순에 있으면 그 슬롯에 강판 투수를 넣는다 (Compact 전담 투수 → 타석 진입)</li>
     *   <li>강판 투수도 타순에 있으면 서로 자리를 바꾼다 (Full)</li>
     * </ul>
     *
     * @param oldPitcherUserId 강판 투수
     * @param newPitcherUserId 신임 투수 (기존 타자)
     */
    public void swapDefendingBattingOrderOnPitcherSubstitute(
            Long oldPitcherUserId,
            Long newPitcherUserId) {
        ensureCollectionsInitialized();
        if (oldPitcherUserId == null || newPitcherUserId == null) {
            return;
        }

        boolean homeDefending = isHomeDefending();
        List<Long> battingOrder = homeDefending
                ? homeBattingOrderUserIds
                : awayBattingOrderUserIds;

        // 레거시: 타순 필드가 비어 있으면 로스터에서 타순을 복제해 둔다
        if (battingOrder.isEmpty()) {
            List<Long> roster = homeDefending ? homeRosterUserIds : awayRosterUserIds;
            battingOrder.addAll(roster);
            // 전담 투수(로스터에만 있는 경우)는 타순에서 제외 — 신임이 타순에 있을 때만 교체
            battingOrder.remove(oldPitcherUserId);
        }

        int newPitcherSlot = battingOrder.indexOf(newPitcherUserId);
        if (newPitcherSlot < 0) {
            throw new BadRequestException(
                    ErrorCode.GAME_PITCHER_SUBSTITUTE_INVALID,
                    "new pitcher not in batting order. newPitcher=" + newPitcherUserId);
        }

        int oldPitcherSlot = battingOrder.indexOf(oldPitcherUserId);
        if (oldPitcherSlot >= 0) {
            // Full 등: 타순 내 서로 자리 교환
            battingOrder.set(newPitcherSlot, oldPitcherUserId);
            battingOrder.set(oldPitcherSlot, newPitcherUserId);
        } else {
            // Compact 전담 투수: 신임(타자) 슬롯을 강판 투수가 이어받는다
            battingOrder.set(newPitcherSlot, oldPitcherUserId);
        }
    }

    /**
     * 타석이 종료되면 타순을 한 칸 전진한다.
     *
     * @return 전진 후 타자 userId
     */
    public Long advanceBatterAfterPlateAppearance() {
        ensureCollectionsInitialized();
        if (batterLineup.isEmpty()) {
            throw new BadRequestException(ErrorCode.GAME_LINEUP_EMPTY);
        }
        currentBatterIndex = (currentBatterIndex + 1) % batterLineup.size();
        return batterLineup.get(currentBatterIndex);
    }

    /**
     * 리그 공수 교대 — 상대 팀 active 투수·타순으로 전환한다.
     *
     * <p>셋업·카드 핸드는 유지한다. 타순 인덱스는 팀별로 이어간다.</p>
     *
     * @return 새 등판 투수 userId
     */
    public Long swapRolesForLeague() {
        ensureCollectionsInitialized();
        boolean homeWasDefending = isHomeDefending();

        if (homeWasDefending) {
            // 어웨이가 공격 중이었음 → 인덱스 저장 후 홈이 공격
            this.awayNextBatterIndex = currentBatterIndex;
            this.pitcherUserId = awayActivePitcherUserId;
            this.batterLineup = new ArrayList<>(resolveHomeBattingOrder());
            this.currentBatterIndex = normalizeBatterIndex(homeNextBatterIndex, batterLineup.size());
        } else {
            this.homeNextBatterIndex = currentBatterIndex;
            this.pitcherUserId = homeActivePitcherUserId;
            this.batterLineup = new ArrayList<>(resolveAwayBattingOrder());
            this.currentBatterIndex = normalizeBatterIndex(awayNextBatterIndex, batterLineup.size());
        }

        if (pitcherUserId == null) {
            throw new BadRequestException(ErrorCode.GAME_ROLE_SWAP_UNSUPPORTED, "active pitcher missing");
        }
        if (batterLineup.isEmpty()) {
            throw new BadRequestException(ErrorCode.GAME_LINEUP_EMPTY);
        }
        syncPitcherCardHandFromPlayer();
        return pitcherUserId;
    }

    /**
     * 타순 인덱스를 로스터 크기에 맞게 정규화한다.
     *
     * @param index 저장 인덱스
     * @param size 타순 크기
     * @return 0 ~ size-1
     */
    private int normalizeBatterIndex(int index, int size) {
        if (size <= 0) {
            return 0;
        }
        int normalized = index % size;
        return normalized < 0 ? normalized + size : normalized;
    }

    /**
     * 홈 타순을 반환한다. 레거시 데이터는 로스터로 폴백한다.
     *
     * @return 홈 타순
     */
    public List<Long> resolveHomeBattingOrder() {
        ensureCollectionsInitialized();
        if (!homeBattingOrderUserIds.isEmpty()) {
            return homeBattingOrderUserIds;
        }
        return homeRosterUserIds;
    }

    /**
     * 어웨이 타순을 반환한다. 레거시 데이터는 로스터로 폴백한다.
     *
     * @return 어웨이 타순
     */
    public List<Long> resolveAwayBattingOrder() {
        ensureCollectionsInitialized();
        if (!awayBattingOrderUserIds.isEmpty()) {
            return awayBattingOrderUserIds;
        }
        return awayRosterUserIds;
    }

    /**
     * 유저가 타순(타자) 멤버인지.
     *
     * @param userId 유저 ID
     * @return 타순이면 true
     */
    public boolean isBattingOrderMember(Long userId) {
        return resolveHomeBattingOrder().contains(userId)
                || resolveAwayBattingOrder().contains(userId);
    }

    /**
     * 타순 + 선발 투수를 출전 전원으로 합친다.
     *
     * @param battingOrder 타순
     * @param startingPitcherUserId 선발 투수
     * @return 출전 전원
     */
    private static List<Long> mergeRoster(List<Long> battingOrder, Long startingPitcherUserId) {
        List<Long> roster = new ArrayList<>(battingOrder);
        if (startingPitcherUserId != null && !roster.contains(startingPitcherUserId)) {
            roster.add(startingPitcherUserId);
        }
        return roster;
    }

    /**
     * 리그 결과 반영 완료로 표시한다.
     */
    public void markLeagueResultApplied() {
        this.leagueResultApplied = true;
    }

    /**
     * 유저의 셋업 엔트리를 찾는다.
     *
     * @param userId 유저 ID
     * @return 엔트리 또는 null
     */
    private PlayerSetupNumbers findPlayerSetupNumbers(Long userId) {
        for (PlayerSetupNumbers entry : ensurePlayerSetupNumbers()) {
            if (userId.equals(entry.getUserId())) {
                return entry;
            }
        }
        return null;
    }

    /** 투수별 아웃 유발 번호 — userId → outNumList */
    public Map<Long, List<Integer>> getOutNumbers() {
        return toNumberMap(PlayerSetupNumbers::getOutNumList);
    }

    /** 투수별 병살 유발 번호 — userId → dpNumList */
    public Map<Long, List<Integer>> getDpNumbers() {
        return toNumberMap(PlayerSetupNumbers::getDpNumList);
    }

    /** 타자별 3루타 유발 번호 — userId → tripleNumList */
    public Map<Long, List<Integer>> getTripleNumbers() {
        return toNumberMap(PlayerSetupNumbers::getTripleNumList);
    }

    /** 타자별 홈런 유발 번호 — userId → hrNumList */
    public Map<Long, List<Integer>> getHrNumbers() {
        return toNumberMap(PlayerSetupNumbers::getHrNumList);
    }

    private Map<Long, List<Integer>> toNumberMap(
            java.util.function.Function<PlayerSetupNumbers, List<Integer>> extractor) {
        Map<Long, List<Integer>> map = new HashMap<>();
        for (PlayerSetupNumbers entry : ensurePlayerSetupNumbers()) {
            map.put(entry.getUserId(), new ArrayList<>(extractor.apply(entry)));
        }
        return map;
    }

    private List<PlayerSetupNumbers> ensurePlayerSetupNumbers() {
        if (playerSetupNumbers == null) {
            playerSetupNumbers = new ArrayList<>();
        }
        return playerSetupNumbers;
    }

    @Builder
    public MatchInfo(String id, GameMode gameMode, Long pitcherUserId, List<Long> batterLineup) {
        this.id = id;
        this.gameMode = gameMode;
        this.matchStatus = GameStatus.WAITING;
        this.pitcherUserId = pitcherUserId;
        this.homeUserId = pitcherUserId;
        this.awayUserId = batterLineup != null && !batterLineup.isEmpty() ? batterLineup.get(0) : null;
        this.batterLineup = batterLineup != null ? batterLineup : new ArrayList<>();
        this.currentBatterIndex = 0;
        this.pitcherCardHand = new ArrayList<>();
        this.mulliganDone = false;
        this.playerCardHands = new ArrayList<>();
        this.mulliganDoneUserIds = new ArrayList<>();
        this.usedAsPitcherIds = new ArrayList<>();
        this.playerSetupNumbers = new ArrayList<>();
        this.homeRosterUserIds = new ArrayList<>();
        this.awayRosterUserIds = new ArrayList<>();
        this.homeBattingOrderUserIds = new ArrayList<>();
        this.awayBattingOrderUserIds = new ArrayList<>();
        this.playerDropCards = new ArrayList<>();
        this.homeActivePitcherUserId = null;
        this.awayActivePitcherUserId = null;
        this.homeNextBatterIndex = 0;
        this.awayNextBatterIndex = 0;
        this.homePitcherSubstitutionCount = 0;
        this.awayPitcherSubstitutionCount = 0;
        this.practiceBotMatch = false;
        this.botDifficulty = null;
        this.botUserId = null;
        this.botDrawPool = new ArrayList<>();
    }

    /**
     * 연습용 봇 매치로 표시한다.
     *
     * <p>결과 경로에서 보상·래더 반영을 건너뛸 때 사용한다.</p>
     */
    public void markAsPracticeBotMatch() {
        this.practiceBotMatch = true;
    }

    /**
     * 연습용 봇 매치 메타(난이도·봇 유저·드로우 풀)를 설정한다.
     *
     * @param difficulty 봇 난이도
     * @param botUserId  봇 유저 ID
     * @param botDrawPool 봇 드로우/멀리건용 인스턴스 ID 목록
     */
    public void markAsPracticeBotMatch(BotDifficulty difficulty, Long botUserId, List<Long> botDrawPool) {
        this.practiceBotMatch = true;
        this.botDifficulty = difficulty;
        this.botUserId = botUserId;
        this.botDrawPool = botDrawPool != null ? new ArrayList<>(botDrawPool) : new ArrayList<>();
    }

    /**
     * 해당 유저가 이 매치의 연습 봇인지.
     *
     * @param userId 유저 ID
     * @return practice bot 매치이고 botUserId와 일치하면 true
     */
    public boolean isPracticeBotUser(Long userId) {
        return practiceBotMatch && botUserId != null && botUserId.equals(userId);
    }

    /**
     * 봇 전용 드로우 풀이 사용 가능한지.
     *
     * @return botDrawPool이 비어 있지 않으면 true
     */
    public boolean hasBotDrawPool() {
        ensureCollectionsInitialized();
        return botDrawPool != null && !botDrawPool.isEmpty();
    }

    /**
     * 리그 매치를 생성한다. 홈 선발 투수로 시작하며, 어웨이 타순이 초기 타순이다.
     *
     * @param id 매치 세션 ID
     * @param gameMode COMPACT_LEAGUE / FULL_LEAGUE
     * @param leagueTier 리그 단계
     * @param homeTeamId 홈 팀 ID (선진입)
     * @param awayTeamId 어웨이 팀 ID (후진입)
     * @param homeLeaderUserId 홈 팀 리더
     * @param awayLeaderUserId 어웨이 팀 리더
     * @param homeStartingPitcherUserId 홈 선발 투수
     * @param awayStartingPitcherUserId 어웨이 선발 투수
     * @param homeBattingOrder 홈 타순
     * @param awayBattingOrder 어웨이 타순
     * @return 리그 MatchInfo
     */
    public static MatchInfo createLeague(
            String id,
            GameMode gameMode,
            LeagueTier leagueTier,
            Long homeTeamId,
            Long awayTeamId,
            Long homeLeaderUserId,
            Long awayLeaderUserId,
            Long homeStartingPitcherUserId,
            Long awayStartingPitcherUserId,
            List<Long> homeBattingOrder,
            List<Long> awayBattingOrder) {

        MatchInfo matchInfo = new MatchInfo();
        matchInfo.id = id;
        matchInfo.gameMode = gameMode;
        matchInfo.matchStatus = GameStatus.WAITING;
        matchInfo.leagueTier = leagueTier;
        matchInfo.homeTeamId = homeTeamId;
        matchInfo.awayTeamId = awayTeamId;
        matchInfo.leagueResultApplied = false;
        matchInfo.homeUserId = homeLeaderUserId;
        matchInfo.awayUserId = awayLeaderUserId;
        // 홈이 먼저 수비(투수), 어웨이가 공격(타순)
        matchInfo.homeStartingPitcherUserId = homeStartingPitcherUserId;
        matchInfo.awayStartingPitcherUserId = awayStartingPitcherUserId;
        matchInfo.homeActivePitcherUserId = homeStartingPitcherUserId;
        matchInfo.awayActivePitcherUserId = awayStartingPitcherUserId;
        matchInfo.pitcherUserId = homeStartingPitcherUserId;
        matchInfo.batterLineup = new ArrayList<>(awayBattingOrder);
        matchInfo.homeBattingOrderUserIds = new ArrayList<>(homeBattingOrder);
        matchInfo.awayBattingOrderUserIds = new ArrayList<>(awayBattingOrder);
        matchInfo.homeRosterUserIds = mergeRoster(homeBattingOrder, homeStartingPitcherUserId);
        matchInfo.awayRosterUserIds = mergeRoster(awayBattingOrder, awayStartingPitcherUserId);
        matchInfo.currentBatterIndex = 0;
        matchInfo.homeNextBatterIndex = 0;
        matchInfo.awayNextBatterIndex = 0;
        matchInfo.homePitcherSubstitutionCount = 0;
        matchInfo.awayPitcherSubstitutionCount = 0;
        matchInfo.pitcherCardHand = new ArrayList<>();
        matchInfo.mulliganDone = false;
        matchInfo.playerCardHands = new ArrayList<>();
        matchInfo.mulliganDoneUserIds = new ArrayList<>();
        matchInfo.usedAsPitcherIds = new ArrayList<>();
        matchInfo.playerSetupNumbers = new ArrayList<>();
        matchInfo.playerDropCards = new ArrayList<>();
        return matchInfo;
    }

    /**
     * 플레이어 1명의 블러핑 숫자 묶음.
     * Redis nested List 직렬화를 위해 Map 대신 사용한다.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class PlayerSetupNumbers {

        private Long userId;
        private List<Integer> outNumList = new ArrayList<>();
        private List<Integer> dpNumList = new ArrayList<>();
        private List<Integer> tripleNumList = new ArrayList<>();
        private List<Integer> hrNumList = new ArrayList<>();

        PlayerSetupNumbers(Long userId,
                           List<Integer> outNumList,
                           List<Integer> dpNumList,
                           List<Integer> tripleNumList,
                           List<Integer> hrNumList) {
            this.userId = userId;
            this.outNumList = new ArrayList<>(outNumList);
            this.dpNumList = new ArrayList<>(dpNumList);
            this.tripleNumList = new ArrayList<>(tripleNumList);
            this.hrNumList = new ArrayList<>(hrNumList);
        }

        void ensureListsInitialized() {
            if (outNumList == null) {
                outNumList = new ArrayList<>();
            }
            if (dpNumList == null) {
                dpNumList = new ArrayList<>();
            }
            if (tripleNumList == null) {
                tripleNumList = new ArrayList<>();
            }
            if (hrNumList == null) {
                hrNumList = new ArrayList<>();
            }
        }

        /** Redis 역직렬화용 */
        void setUserId(Long userId) {
            this.userId = userId;
        }

        void setOutNumList(List<Integer> outNumList) {
            this.outNumList = outNumList != null ? new ArrayList<>(outNumList) : new ArrayList<>();
        }

        void setDpNumList(List<Integer> dpNumList) {
            this.dpNumList = dpNumList != null ? new ArrayList<>(dpNumList) : new ArrayList<>();
        }

        void setTripleNumList(List<Integer> tripleNumList) {
            this.tripleNumList = tripleNumList != null ? new ArrayList<>(tripleNumList) : new ArrayList<>();
        }

        void setHrNumList(List<Integer> hrNumList) {
            this.hrNumList = hrNumList != null ? new ArrayList<>(hrNumList) : new ArrayList<>();
        }
    }

    /**
     * 플레이어 1명의 구종 카드 패.
     * Redis nested List 직렬화를 위해 Map 대신 사용한다.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class PlayerCardHand {

        private Long userId;
        private List<Long> cardIds = new ArrayList<>();

        PlayerCardHand(Long userId, List<Long> cardIds) {
            this.userId = userId;
            this.cardIds = new ArrayList<>(cardIds);
        }

        void ensureListsInitialized() {
            if (cardIds == null) {
                cardIds = new ArrayList<>();
            }
        }

        void setUserId(Long userId) {
            this.userId = userId;
        }

        void setCardIds(List<Long> cardIds) {
            this.cardIds = cardIds != null ? new ArrayList<>(cardIds) : new ArrayList<>();
        }
    }

    /**
     * 플레이어별 투수 교체 시 제외 카드.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class PlayerDropCard {

        private Long userId;
        private Long dropCardId;

        PlayerDropCard(Long userId, Long dropCardId) {
            this.userId = userId;
            this.dropCardId = dropCardId;
        }

        void setUserId(Long userId) {
            this.userId = userId;
        }

        void setDropCardId(Long dropCardId) {
            this.dropCardId = dropCardId;
        }
    }
}
