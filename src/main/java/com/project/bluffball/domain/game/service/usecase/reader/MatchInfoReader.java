package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.enums.SetupKind;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MatchInfo Redis 엔티티 읽기 전담 리더.
 *
 * <p>Service 레이어는 이 클래스의 원시값·ID 반환 메서드만 호출한다.
 * 엔티티(MatchInfo)를 직접 반환하는 {@link #getById}는
 * Executor·Reader 내부에서만 사용한다.</p>
 */
@Component
@RequiredArgsConstructor
public class MatchInfoReader {

    private final MatchInfoRepository matchInfoRepository;
    private final GameModeRule gameModeRule;

    /** Executor·Reader 내부 전용 — Service에서 호출 금지 */
    public MatchInfo getById(String matchSessionId) {
        MatchInfo matchInfo = matchInfoRepository.findById(matchSessionId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.MATCH_SESSION_NOT_FOUND, "matchSessionId=" + matchSessionId));
        matchInfo.ensureCollectionsInitialized();
        return matchInfo;
    }

    /** 현재 투수 카드 패 ID 목록 반환 (Service ✅) */
    public List<Long> getPitcherCardHand(String matchSessionId) {
        return getById(matchSessionId).getPitcherCardHand();
    }

    /** 게임 모드 반환 (Service ✅) */
    public GameMode getGameMode(String matchSessionId) {
        return getById(matchSessionId).getGameMode();
    }

    /** 멀리건 완료 여부 — 모든 참가자 완료 시 true (Service ✅) */
    public boolean isMulliganDone(String matchSessionId) {
        MatchInfo matchInfo = getById(matchSessionId);
        // 리그: 인게임 멀리건 없음 → 항상 완료로 간주
        if (!gameModeRule.usesInGameCardDrawAndMulligan(matchInfo.getGameMode())) {
            return true;
        }
        return matchInfo.isAllMulliganDone(matchInfo.getParticipantUserIds());
    }

    /** 특정 참가자의 멀리건 완료 여부 (Service ✅) */
    public boolean isMulliganDoneForUser(String matchSessionId, Long userId) {
        MatchInfo matchInfo = getById(matchSessionId);
        if (!gameModeRule.usesInGameCardDrawAndMulligan(matchInfo.getGameMode())) {
            return true;
        }
        return matchInfo.isMulliganDoneForUser(userId);
    }

    /** 매치 참가자 userId 목록 (Service ✅) */
    public List<Long> getParticipantUserIds(String matchSessionId) {
        return getById(matchSessionId).getParticipantUserIds();
    }

    /** 플레이어 카드 패 ID 목록 (Service ✅) */
    public List<Long> getPlayerCardHand(String matchSessionId, Long userId) {
        return getById(matchSessionId).getPlayerCardHand(userId);
    }

    /** 현재 등판 투수 userId 반환 (Service ✅) */
    public Long getPitcherUserId(String matchSessionId) {
        return getById(matchSessionId).getPitcherUserId();
    }

    /** 홈팀 userId — 매치 생성 시 고정 (Service ✅) */
    public Long getHomeUserId(String matchSessionId) {
        MatchInfo matchInfo = getById(matchSessionId);
        return matchInfo.getHomeUserId() != null
                ? matchInfo.getHomeUserId()
                : matchInfo.getPitcherUserId();
    }

    /** 어웨이팀 userId — 매치 생성 시 고정 (Service ✅) */
    public Long getAwayUserId(String matchSessionId) {
        MatchInfo matchInfo = getById(matchSessionId);
        if (matchInfo.getAwayUserId() != null) {
            return matchInfo.getAwayUserId();
        }
        List<Long> lineup = matchInfo.getBatterLineup();
        if (lineup.isEmpty()) {
            throw new BadRequestException(ErrorCode.GAME_LINEUP_EMPTY, "matchSessionId=" + matchSessionId);
        }
        return lineup.get(0);
    }

    /** 현재 타석 타자 userId 반환 (Service ✅) */
    public Long getCurrentBatterUserId(String matchSessionId) {
        MatchInfo matchInfo = getById(matchSessionId);
        List<Long> lineup = matchInfo.getBatterLineup();
        if (lineup.isEmpty()) {
            throw new BadRequestException(ErrorCode.GAME_LINEUP_EMPTY, "matchSessionId=" + matchSessionId);
        }
        return lineup.get(matchInfo.getCurrentBatterIndex());
    }

    /**
     * 유저가 해당 매치의 참가자인지 반환한다. (Service·WebSocket Security ✅)
     *
     * <p>현재 투수, 타순 라인업, 과거 투수 등판 이력 중 하나에 포함되면 참가자로 본다.
     * 매치가 없으면 {@code false}를 반환한다.</p>
     *
     * @param matchSessionId 매치 세션 ID
     * @param userId         검증 대상 유저 ID
     * @return 참가자이면 {@code true}
     */
    public boolean isParticipant(String matchSessionId, Long userId) {
        return matchInfoRepository.findById(matchSessionId)
                .map(matchInfo -> {
                    matchInfo.ensureCollectionsInitialized();
                    return containsParticipant(matchInfo, userId);
                })
                .orElse(false);
    }

    private boolean containsParticipant(MatchInfo matchInfo, Long userId) {
        // 리그: 양 팀 로스터
        if (matchInfo.getHomeRosterUserIds().contains(userId)
                || matchInfo.getAwayRosterUserIds().contains(userId)) {
            return true;
        }
        if (userId.equals(matchInfo.getPitcherUserId())) {
            return true;
        }
        if (matchInfo.getBatterLineup().contains(userId)) {
            return true;
        }
        return matchInfo.getUsedAsPitcherIds().contains(userId);
    }

    /**
     * 블러핑 숫자 제출이 모드 규칙상 완료됐는지 확인한다. (Service ✅)
     *
     * <p>쇼다운: 참가 2명의 FULL 셋업.
     * 리그: 현재 투수(+상대 선발 투수)의 투수 셋업 + 로스터 전원의 타자 셋업.</p>
     *
     * @param matchSessionId 매치 세션 ID
     * @return 완료 여부
     */
    public boolean isSetupNumbersComplete(String matchSessionId) {
        MatchInfo matchInfo = getById(matchSessionId);
        GameMode gameMode = matchInfo.getGameMode();
        if (gameModeRule.usesInGameCardDrawAndMulligan(gameMode)) {
            int required = gameModeRule.getRequiredSetupCount(gameMode);
            long fullCount = matchInfo.getParticipantUserIds().stream()
                    .filter(matchInfo::hasFullSetup)
                    .count();
            return fullCount >= required;
        }
        return isLeagueSetupComplete(matchInfo);
    }

    /**
     * 유저가 다음에 제출해야 할 셋업 종류를 반환한다. (Service ✅)
     *
     * <p>완료됐으면 {@code null}.</p>
     *
     * @param matchSessionId 매치 세션 ID
     * @param userId 유저 ID
     * @return SetupKind 또는 null
     */
    public SetupKind getRequiredSetupKind(String matchSessionId, Long userId) {
        MatchInfo matchInfo = getById(matchSessionId);
        GameMode gameMode = matchInfo.getGameMode();
        if (gameModeRule.usesInGameCardDrawAndMulligan(gameMode)) {
            return matchInfo.hasFullSetup(userId) ? null : SetupKind.FULL;
        }
        if (needsLeaguePitcherSetup(matchInfo, userId) && !matchInfo.hasPitcherSetup(userId)) {
            return SetupKind.PITCHER;
        }
        if (matchInfo.isBattingOrderMember(userId) && !matchInfo.hasBatterSetup(userId)) {
            return SetupKind.BATTER;
        }
        return null;
    }

    /**
     * 유저가 해당 매치에서 제출 가능한 셋업 종류인지 확인한다. (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @param userId 유저 ID
     * @param setupKind 제출하려는 종류
     * @return 허용되면 true
     */
    public boolean canSubmitSetupKind(String matchSessionId, Long userId, SetupKind setupKind) {
        SetupKind required = getRequiredSetupKind(matchSessionId, userId);
        return required != null && required == setupKind;
    }

    /**
     * 리그 셋업 완료 여부.
     *
     * @param matchInfo 매치 정보
     * @return 완료면 true
     */
    private boolean isLeagueSetupComplete(MatchInfo matchInfo) {
        Long pitcherUserId = matchInfo.getPitcherUserId();
        if (pitcherUserId == null || !matchInfo.hasPitcherSetup(pitcherUserId)) {
            return false;
        }
        // 상대 선발 투수 셋업(공수 교대 대비) — 현재 투수와 다를 때만
        Long reservePitcher = resolveReserveStartingPitcher(matchInfo);
        if (reservePitcher != null && !matchInfo.hasPitcherSetup(reservePitcher)) {
            return false;
        }
        for (Long userId : matchInfo.resolveHomeBattingOrder()) {
            if (!matchInfo.hasBatterSetup(userId)) {
                return false;
            }
        }
        for (Long userId : matchInfo.resolveAwayBattingOrder()) {
            if (!matchInfo.hasBatterSetup(userId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 리그에서 투수 셋업이 필요한 유저인지.
     *
     * @param matchInfo 매치 정보
     * @param userId 유저 ID
     * @return 필요하면 true
     */
    private boolean needsLeaguePitcherSetup(MatchInfo matchInfo, Long userId) {
        if (userId.equals(matchInfo.getPitcherUserId())) {
            return true;
        }
        Long reservePitcher = resolveReserveStartingPitcher(matchInfo);
        return userId.equals(reservePitcher);
    }

    /**
     * 현재 마운드가 아닌 쪽의 active 투수(공수 교대 대비).
     *
     * @param matchInfo 매치 정보
     * @return 예비 투수 ID 또는 null
     */
    private Long resolveReserveStartingPitcher(MatchInfo matchInfo) {
        Long pitcherUserId = matchInfo.getPitcherUserId();
        Long homeActive = matchInfo.getHomeActivePitcherUserId();
        Long awayActive = matchInfo.getAwayActivePitcherUserId();
        if (homeActive == null) {
            homeActive = matchInfo.getHomeStartingPitcherUserId();
        }
        if (awayActive == null) {
            awayActive = matchInfo.getAwayStartingPitcherUserId();
        }
        if (matchInfo.isHomeDefending()) {
            return awayActive != null && !awayActive.equals(pitcherUserId) ? awayActive : null;
        }
        return homeActive != null && !homeActive.equals(pitcherUserId) ? homeActive : null;
    }

    /**
     * 홈 로스터 소속 여부.
     *
     * @param matchInfo 매치 정보
     * @param userId 유저 ID
     * @return 홈이면 true
     */
    private boolean isOnHomeRoster(MatchInfo matchInfo, Long userId) {
        return matchInfo.getHomeRosterUserIds().contains(userId);
    }

    /**
     * 어웨이 로스터 소속 여부.
     *
     * @param matchInfo 매치 정보
     * @param userId 유저 ID
     * @return 어웨이면 true
     */
    private boolean isOnAwayRoster(MatchInfo matchInfo, Long userId) {
        return matchInfo.getAwayRosterUserIds().contains(userId);
    }

    /**
     * 이미 투수로 등판한 유저 ID 목록 (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 등판 이력
     */
    public List<Long> getUsedAsPitcherIds(String matchSessionId) {
        return List.copyOf(getById(matchSessionId).getUsedAsPitcherIds());
    }

    /**
     * 홈 팀 ID (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 홈 팀 ID 또는 null
     */
    public Long getHomeTeamId(String matchSessionId) {
        return getById(matchSessionId).getHomeTeamId();
    }

    /**
     * 어웨이 팀 ID (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 어웨이 팀 ID 또는 null
     */
    public Long getAwayTeamId(String matchSessionId) {
        return getById(matchSessionId).getAwayTeamId();
    }

    /**
     * 리그 결과 반영 여부 (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 반영됐으면 true
     */
    public boolean isLeagueResultApplied(String matchSessionId) {
        return getById(matchSessionId).isLeagueResultApplied();
    }

    /**
     * 리그 매치 여부 (팀 ID 존재) (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 리그 매치면 true
     */
    public boolean isLeagueMatch(String matchSessionId) {
        MatchInfo matchInfo = getById(matchSessionId);
        return matchInfo.getHomeTeamId() != null && matchInfo.getAwayTeamId() != null;
    }

    /**
     * 홈 선발 투수 ID (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 홈 선발 투수
     */
    public Long getHomeStartingPitcherUserId(String matchSessionId) {
        return getById(matchSessionId).getHomeStartingPitcherUserId();
    }

    /**
     * 어웨이 선발 투수 ID (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 어웨이 선발 투수
     */
    public Long getAwayStartingPitcherUserId(String matchSessionId) {
        return getById(matchSessionId).getAwayStartingPitcherUserId();
    }

    /**
     * 투수 교체 횟수 — 현재 수비 팀 기준 (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 교체 횟수
     */
    public int getPitcherSubstitutionCount(String matchSessionId) {
        return getById(matchSessionId).getDefendingPitcherSubstitutionCount();
    }

    /**
     * 홈 현재 등판 투수 (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 홈 active 투수
     */
    public Long getHomeActivePitcherUserId(String matchSessionId) {
        return getById(matchSessionId).getHomeActivePitcherUserId();
    }

    /**
     * 어웨이 현재 등판 투수 (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 어웨이 active 투수
     */
    public Long getAwayActivePitcherUserId(String matchSessionId) {
        return getById(matchSessionId).getAwayActivePitcherUserId();
    }

    /**
     * dropCardId (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @param userId 유저 ID
     * @return dropCardId 또는 null
     */
    public Long getPlayerDropCard(String matchSessionId, Long userId) {
        return getById(matchSessionId).getPlayerDropCard(userId);
    }

    /**
     * 홈 로스터 (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 홈 로스터
     */
    public List<Long> getHomeRosterUserIds(String matchSessionId) {
        return List.copyOf(getById(matchSessionId).getHomeRosterUserIds());
    }

    /**
     * 어웨이 로스터 (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 어웨이 로스터
     */
    public List<Long> getAwayRosterUserIds(String matchSessionId) {
        return List.copyOf(getById(matchSessionId).getAwayRosterUserIds());
    }

    /**
     * 홈 타순 (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 홈 타순
     */
    public List<Long> getHomeBattingOrderUserIds(String matchSessionId) {
        return List.copyOf(getById(matchSessionId).resolveHomeBattingOrder());
    }

    /**
     * 어웨이 타순 (Service ✅)
     *
     * @param matchSessionId 매치 세션 ID
     * @return 어웨이 타순
     */
    public List<Long> getAwayBattingOrderUserIds(String matchSessionId) {
        return List.copyOf(getById(matchSessionId).resolveAwayBattingOrder());
    }

    /** 투수 아웃 유발 블러핑 숫자 (Executor·Calculator 내부용) */
    public List<Integer> getOutNumbers(String matchSessionId, Long pitcherUserId) {
        return getBluffingNumbers(getById(matchSessionId).getOutNumbers(), pitcherUserId);
    }

    /** 투수 병살 유발 블러핑 숫자 (Executor·Calculator 내부용) */
    public List<Integer> getDpNumbers(String matchSessionId, Long pitcherUserId) {
        return getBluffingNumbers(getById(matchSessionId).getDpNumbers(), pitcherUserId);
    }

    /** 타자 3루타 유발 블러핑 숫자 (Executor·Calculator 내부용) */
    public List<Integer> getTripleNumbers(String matchSessionId, Long batterUserId) {
        return getBluffingNumbers(getById(matchSessionId).getTripleNumbers(), batterUserId);
    }

    /** 타자 홈런 유발 블러핑 숫자 (Executor·Calculator 내부용) */
    public List<Integer> getHrNumbers(String matchSessionId, Long batterUserId) {
        return getBluffingNumbers(getById(matchSessionId).getHrNumbers(), batterUserId);
    }

    /** 2루타 판정 목표 주사위 눈금 (1~6) */
    public int getDoubleJudgmentTargetFace(String matchSessionId) {
        MatchInfo matchInfo = getById(matchSessionId);
        if (!matchInfo.isDoubleJudgmentConfigured()) {
            throw new BadRequestException(
                    ErrorCode.GAME_DOUBLE_JUDGMENT_NOT_CONFIGURED, "matchSessionId=" + matchSessionId);
        }
        return matchInfo.getDoubleJudgmentTargetFace();
    }

    /** 2루타 판정 시 앞 주사위(true) / 뒷 주사위(false) 사용 여부 */
    public boolean isDoubleJudgmentUseFrontDice(String matchSessionId) {
        MatchInfo matchInfo = getById(matchSessionId);
        if (!matchInfo.isDoubleJudgmentConfigured()) {
            throw new BadRequestException(
                    ErrorCode.GAME_DOUBLE_JUDGMENT_NOT_CONFIGURED, "matchSessionId=" + matchSessionId);
        }
        return matchInfo.isDoubleJudgmentUseFrontDice();
    }

    private List<Integer> getBluffingNumbers(Map<Long, List<Integer>> numbersByUserId, Long userId) {
        List<Integer> numbers = numbersByUserId.get(userId);
        return numbers != null ? numbers : Collections.emptyList();
    }
}
