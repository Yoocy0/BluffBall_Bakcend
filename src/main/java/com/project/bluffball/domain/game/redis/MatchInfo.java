package com.project.bluffball.domain.game.redis;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.enums.GameStatus;
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

    /** 리그 티어 — 리그 매치에서 사용 */
    @Enumerated(EnumType.ORDINAL)
    private LeagueTier leagueTier;

    /** 홈 출전 로스터 — 리그 매치 */
    private List<Long> homeRosterUserIds;

    /** 어웨이 출전 로스터 — 리그 매치 */
    private List<Long> awayRosterUserIds;

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
        for (PlayerSetupNumbers entry : playerSetupNumbers) {
            entry.ensureListsInitialized();
        }
        for (PlayerCardHand entry : playerCardHands) {
            entry.ensureListsInitialized();
        }
    }

    /** SetupNumberExecutor 전용 — 플레이어 제출 upsert */
    public void upsertPlayerSetupNumbers(Long userId, SetupNumberRequest request) {
        ensureCollectionsInitialized();
        playerSetupNumbers.removeIf(entry -> userId.equals(entry.getUserId()));
        playerSetupNumbers.add(new PlayerSetupNumbers(
                userId,
                request.outNumList(),
                request.dpNumList(),
                request.tripleNumList(),
                request.hrNumList()));
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
    }

    /**
     * 리그 매치를 생성한다. 선발 투수·타순은 이후 starting-lineup으로 확정한다.
     *
     * @param id 매치 세션 ID
     * @param gameMode COMPACT_LEAGUE / FULL_LEAGUE
     * @param leagueTier 리그 단계
     * @param homeTeamId 홈 팀 ID (선진입)
     * @param awayTeamId 어웨이 팀 ID (후진입)
     * @param homeLeaderUserId 홈 팀 리더
     * @param awayLeaderUserId 어웨이 팀 리더
     * @param homeRoster 홈 출전 로스터
     * @param awayRoster 어웨이 출전 로스터
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
            List<Long> homeRoster,
            List<Long> awayRoster) {

        MatchInfo matchInfo = new MatchInfo();
        matchInfo.id = id;
        matchInfo.gameMode = gameMode;
        matchInfo.matchStatus = GameStatus.WAITING;
        matchInfo.leagueTier = leagueTier;
        matchInfo.homeTeamId = homeTeamId;
        matchInfo.awayTeamId = awayTeamId;
        matchInfo.homeUserId = homeLeaderUserId;
        matchInfo.awayUserId = awayLeaderUserId;
        // 선발 확정 전 placeholder — 홈 리더를 임시 투수, 어웨이 로스터를 임시 타순
        matchInfo.pitcherUserId = homeLeaderUserId;
        matchInfo.batterLineup = new ArrayList<>(awayRoster);
        matchInfo.homeRosterUserIds = new ArrayList<>(homeRoster);
        matchInfo.awayRosterUserIds = new ArrayList<>(awayRoster);
        matchInfo.currentBatterIndex = 0;
        matchInfo.pitcherCardHand = new ArrayList<>();
        matchInfo.mulliganDone = false;
        matchInfo.playerCardHands = new ArrayList<>();
        matchInfo.mulliganDoneUserIds = new ArrayList<>();
        matchInfo.usedAsPitcherIds = new ArrayList<>();
        matchInfo.playerSetupNumbers = new ArrayList<>();
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
}
