package com.project.bluffball.domain.game.redis;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
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

    /** 플레이어별 블러핑 숫자 — Redis에 List로 저장 */
    private List<PlayerSetupNumbers> playerSetupNumbers;

    /** 멀리건 완료 처리 — MulliganExecutor에서만 호출한다. */
    public void completeMulligan() {
        this.mulliganDone = true;
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
        for (PlayerSetupNumbers entry : playerSetupNumbers) {
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
        this.batterLineup = batterLineup != null ? batterLineup : new ArrayList<>();
        this.currentBatterIndex = 0;
        this.pitcherCardHand = new ArrayList<>();
        this.mulliganDone = false;
        this.usedAsPitcherIds = new ArrayList<>();
        this.playerSetupNumbers = new ArrayList<>();
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
}
