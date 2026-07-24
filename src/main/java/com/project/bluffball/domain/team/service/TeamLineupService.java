package com.project.bluffball.domain.team.service;

import com.project.bluffball.domain.card.service.usecase.reader.CardReader;
import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.league.enums.LeagueFormat;
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
    private final CardReader cardReader;
    private final GameModeRule gameModeRule;

    private final TeamLineupExecutor teamLineupExecutor;
    private final TeamPitchCardsExecutor teamPitchCardsExecutor;

    /**
     * 출전 로스터(타순·선발 투수)를 저장한다. 리더만 가능하다.
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

        List<Long> userIds = request.userIds();
        int requiredSize = gameModeRule.getRosterSize(toGameMode(format));
        teamLineupValidator.validateSize(userIds.size(), requiredSize);
        teamLineupValidator.validateNoDuplicates(userIds);
        teamLineupValidator.validateAllMembers(teamMemberReader.areAllMembers(teamId, userIds));
        teamLineupValidator.validateStartingPitcherInRoster(userIds, request.startingPitcherUserId());

        Long savedTeamId = teamLineupExecutor.upsert(
                teamId, format, userIds, request.startingPitcherUserId());
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
     * 멤버별 구종·강화 사전 선택을 저장한다. 리더만 가능하다.
     *
     * @param userId 요청 유저 ID
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param request 멤버별 선택
     * @return 저장된 사전 선택
     */
    public TeamPitchCardsResponse upsertPitchCards(
            Long userId,
            Long teamId,
            LeagueFormat format,
            UpsertTeamPitchCardsRequest request) {

        teamReader.getTeamResponse(teamId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, userId));

        // 로스터가 먼저 있어야 함
        List<Long> lineupUserIds = teamLineupReader.getUserIds(teamId, format);
        List<Long> selectionUserIds = request.selections().stream()
                .map(UpsertTeamPitchCardsRequest.MemberPitchCardSelection::userId)
                .toList();
        teamPitchCardsValidator.validateMembersMatchLineup(selectionUserIds, lineupUserIds);

        int requiredHandSize = gameModeRule.getHandSize(toGameMode(format));
        for (UpsertTeamPitchCardsRequest.MemberPitchCardSelection selection : request.selections()) {
            teamPitchCardsValidator.validateHandSize(selection.cardIds().size(), requiredHandSize);
            teamPitchCardsValidator.validateDropCardInHand(selection.cardIds(), selection.dropCardId());
            teamPitchCardsValidator.validateCardsValid(
                    cardReader.areValidPitcherHandCards(selection.cardIds()));
        }

        Long savedTeamId = teamPitchCardsExecutor.replaceAll(teamId, format, request.selections());
        return teamPitchCardsReader.getPitchCardsResponse(savedTeamId, format);
    }

    /**
     * 멤버별 구종·강화 사전 선택을 조회한다.
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
