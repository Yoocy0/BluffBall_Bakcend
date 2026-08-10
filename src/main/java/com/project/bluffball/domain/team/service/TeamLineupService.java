package com.project.bluffball.domain.team.service;

import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.team.dto.request.UpsertMyPitchCardsRequest;
import com.project.bluffball.domain.team.dto.request.UpsertTeamLineupRequest;
import com.project.bluffball.domain.team.dto.request.UpsertTeamPitchCardsRequest;
import com.project.bluffball.domain.team.dto.response.TeamLineupResponse;
import com.project.bluffball.domain.team.dto.response.TeamPitchCardsResponse;
import com.project.bluffball.domain.team.service.usecase.executor.TeamLineupExecutor;
import com.project.bluffball.domain.team.service.usecase.executor.TeamPitchCardsExecutor;
import com.project.bluffball.domain.team.service.usecase.reader.TeamLineupReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamMemberReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamPitchCardsReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import com.project.bluffball.domain.team.service.usecase.validator.TeamLineupValidator;
import com.project.bluffball.domain.team.service.usecase.validator.TeamMembershipValidator;
import com.project.bluffball.domain.team.service.usecase.validator.TeamPitchCardsValidator;
import com.project.bluffball.domain.user.record.enums.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 리그 출전 로스터·구종 사전 선택 서비스.
 *
 * <p>Entity·Repository에 직접 접근하지 않고 usecase를 조립한다.</p>
 */
@Service
@RequiredArgsConstructor
public class TeamLineupService {

    private final TeamMembershipValidator teamMembershipValidator;
    private final TeamLineupValidator teamLineupValidator;
    private final TeamPitchCardsValidator teamPitchCardsValidator;

    private final TeamReader teamReader;
    private final TeamMemberReader teamMemberReader;
    private final TeamLineupReader teamLineupReader;
    private final TeamPitchCardsReader teamPitchCardsReader;
    private final UserPitchCardReader userPitchCardReader;
    private final GameModeRule gameModeRule;

    private final TeamLineupExecutor teamLineupExecutor;
    private final TeamPitchCardsExecutor teamPitchCardsExecutor;

    /**
     * 출전 로스터(타순·선발 투수)를 저장한다. 리더만 가능하다.
     *
     * <p>Compact: 타순 3명 + 전담 투수 1명(타순 밖). Full: 타순 9명에 선발 투수 포함.</p>
     *
     * @param userId 요청 유저 ID
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param request 타순·선발 투수
     * @return 저장된 로스터
     */
    public TeamLineupResponse upsertLineup(
            Long userId,
            Long teamId,
            LeagueFormat format,
            UpsertTeamLineupRequest request) {

        // 팀 존재·리더 권한
        teamReader.getTeamResponse(teamId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, userId));

        List<Long> battingOrder = request.userIds();
        Long startingPitcherUserId = request.startingPitcherUserId();
        GameMode gameMode = toGameMode(format);

        teamLineupValidator.validateSize(
                battingOrder.size(), gameModeRule.getBattingOrderSize(gameMode));
        teamLineupValidator.validateNoDuplicates(battingOrder);

        if (gameModeRule.hasDedicatedPitcher(gameMode)) {
            teamLineupValidator.validateDedicatedStartingPitcher(battingOrder, startingPitcherUserId);
        } else {
            teamLineupValidator.validateStartingPitcherInRoster(battingOrder, startingPitcherUserId);
        }

        List<Long> matchRoster = TeamLineupReader.mergeMatchRoster(battingOrder, startingPitcherUserId);
        teamLineupValidator.validateNoDuplicates(matchRoster);
        teamLineupValidator.validateAllMembers(teamMemberReader.areAllMembers(teamId, matchRoster));

        Long savedTeamId = teamLineupExecutor.upsert(
                teamId, format, battingOrder, startingPitcherUserId);
        return teamLineupReader.getLineupResponse(savedTeamId, format);
    }

    /**
     * 출전 로스터를 조회한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 로스터
     */
    public TeamLineupResponse getLineup(Long teamId, LeagueFormat format) {
        teamReader.getTeamResponse(teamId);
        return teamLineupReader.getLineupResponse(teamId, format);
    }

    /**
     * 멤버별 구종 사전 선택을 일괄 저장한다. (레거시 — 리더 전용)
     *
     * @deprecated 개인 선택 {@link #upsertMyPitchCards} 사용
     */
    @Deprecated
    public TeamPitchCardsResponse upsertPitchCards(
            Long userId,
            Long teamId,
            LeagueFormat format,
            UpsertTeamPitchCardsRequest request) {

        teamReader.getTeamResponse(teamId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, userId));

        List<Long> matchRosterUserIds = teamLineupReader.getMatchRosterUserIds(teamId, format);
        List<Long> selectionUserIds = request.selections().stream()
                .map(UpsertTeamPitchCardsRequest.MemberPitchCardSelection::userId)
                .toList();
        teamPitchCardsValidator.validateMembersMatchLineup(selectionUserIds, matchRosterUserIds);

        int requiredHandSize = gameModeRule.getHandSize(toGameMode(format));
        for (UpsertTeamPitchCardsRequest.MemberPitchCardSelection selection : request.selections()) {
            validateMemberSelection(
                    selection.userId(),
                    selection.userPitchCardIds(),
                    selection.dropCardId(),
                    requiredHandSize);
        }

        Long savedTeamId = teamPitchCardsExecutor.replaceAll(teamId, format, request.selections());
        return teamPitchCardsReader.getPitchCardsResponse(savedTeamId, format);
    }

    /**
     * 본인 구종 사전 선택을 저장한다. 출전 로스터에 포함된 멤버만 가능하다.
     *
     * <p>카드별 코스트 합 = 모드 핸드 장수. 보유·강화 오버레이 기준.</p>
     *
     * @param userId 요청 유저 ID
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param request 본인 카드 선택
     * @return 팀 전체 사전 선택 현황
     */
    public TeamPitchCardsResponse upsertMyPitchCards(
            Long userId,
            Long teamId,
            LeagueFormat format,
            UpsertMyPitchCardsRequest request) {

        teamReader.getTeamResponse(teamId);
        teamMembershipValidator.validateMember(teamMemberReader.isMember(teamId, userId));

        List<Long> matchRosterUserIds = teamLineupReader.getMatchRosterUserIds(teamId, format);
        teamPitchCardsValidator.validateRequesterInRoster(userId, matchRosterUserIds);

        int requiredHandSize = gameModeRule.getHandSize(toGameMode(format));
        validateMemberSelection(
                userId, request.userPitchCardIds(), request.dropCardId(), requiredHandSize);

        Long savedTeamId = teamPitchCardsExecutor.upsertOne(
                teamId, format, userId, request.userPitchCardIds(), request.dropCardId());
        return teamPitchCardsReader.getPitchCardsResponse(savedTeamId, format);
    }

    /**
     * 멤버 1명 선택 공통 검증 (인스턴스 ID 기준).
     *
     * @param ownerUserId      카드 소유자
     * @param userPitchCardIds 선택 인스턴스
     * @param dropCardId       drop 인스턴스
     * @param requiredHandSize 핸드 장수(코스트 합 목표)
     */
    private void validateMemberSelection(
            Long ownerUserId,
            List<Long> userPitchCardIds,
            Long dropCardId,
            int requiredHandSize) {
        teamPitchCardsValidator.validateDropCardInHand(userPitchCardIds, dropCardId);
        teamPitchCardsValidator.validateCardsValid(
                userPitchCardReader.areValidPitchInstances(ownerUserId, userPitchCardIds));
        teamPitchCardsValidator.validateOwned(
                userPitchCardReader.ownsAllInstances(ownerUserId, userPitchCardIds));
        teamPitchCardsValidator.validateTotalCost(
                userPitchCardReader.sumInstanceCost(ownerUserId, userPitchCardIds), requiredHandSize);
    }

    /**
     * 멤버별 구종 사전 선택을 조회한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 사전 선택
     */
    public TeamPitchCardsResponse getPitchCards(Long teamId, LeagueFormat format) {
        teamReader.getTeamResponse(teamId);
        return teamPitchCardsReader.getPitchCardsResponse(teamId, format);
    }

    /**
     * LeagueFormat → GameMode 매핑.
     *
     * @param format 리그 구분
     * @return 게임 모드
     */
    private GameMode toGameMode(LeagueFormat format) {
        return switch (format) {
            case COMPACT -> GameMode.COMPACT_LEAGUE;
            case FULL -> GameMode.FULL_LEAGUE;
        };
    }
}
