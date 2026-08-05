package com.project.bluffball.domain.league.service;

import com.project.bluffball.domain.league.config.LeagueTierRule;
import com.project.bluffball.domain.league.dto.response.LeagueStandingsResponse;
import com.project.bluffball.domain.league.dto.response.TeamLeagueProgressResponse;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.service.usecase.executor.TeamLeagueProgressExecutor;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueReader;
import com.project.bluffball.domain.league.service.usecase.reader.TeamLeagueProgressReader;
import com.project.bluffball.domain.league.service.usecase.validator.LeagueProgressValidator;
import com.project.bluffball.domain.team.service.usecase.reader.TeamMemberReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import com.project.bluffball.domain.team.service.usecase.validator.TeamMembershipValidator;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 상시 리그 티어 진행 서비스.
 *
 * <p>최초 진입·승급·진행 상태 조회를 조립한다. Entity·Repository에 직접 접근하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class LeagueProgressService {

    private final LeagueProgressValidator leagueProgressValidator;
    private final TeamMembershipValidator teamMembershipValidator;

    private final TeamLeagueProgressReader teamLeagueProgressReader;
    private final LeagueReader leagueReader;
    private final TeamMemberReader teamMemberReader;
    private final TeamReader teamReader;

    private final TeamLeagueProgressExecutor teamLeagueProgressExecutor;

    /**
     * 내 팀의 전 포맷 리그 진행 상태를 조회한다.
     *
     * @param userId 유저 ID
     * @return 진행 상태 목록
     */
    public List<TeamLeagueProgressResponse> getMyProgress(Long userId) {
        Long teamId = requireTeamId(userId);
        return teamLeagueProgressReader.getProgressResponses(teamId);
    }

    /**
     * 포맷별 진행 상태를 조회한다.
     *
     * @param userId 유저 ID
     * @param format 포맷
     * @return 진행 상태
     */
    public TeamLeagueProgressResponse getProgress(Long userId, LeagueFormat format) {
        Long teamId = requireTeamId(userId);
        return teamLeagueProgressReader.getProgressResponse(teamId, format);
    }

    /**
     * 포맷·티어별 리그 전체 순위를 조회한다.
     *
     * @param format 리그 포맷
     * @param tier 리그 티어
     * @return 순위 응답
     */
    public LeagueStandingsResponse getStandings(LeagueFormat format, LeagueTier tier) {
        return teamLeagueProgressReader.getStandingsResponse(format, tier);
    }

    /**
     * 최하위 티어로 최초 진입한다. 리더만 가능하며 참가비를 지불한다.
     *
     * @param userId 유저 ID
     * @param format 포맷
     * @return 진입 후 진행 상태
     */
    public TeamLeagueProgressResponse enter(Long userId, LeagueFormat format) {
        Long teamId = requireTeamId(userId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, userId));

        LeagueTier lowest = LeagueTierRule.lowestTier();
        leagueProgressValidator.validateNotAlreadyEntered(
                teamLeagueProgressReader.exists(teamId, format));
        leagueProgressValidator.validateInitialTier(lowest);

        Long leagueId = leagueReader.getByFormatAndTier(format, lowest).leagueId();
        int minTeamMembers = leagueReader.getMinTeamMembers(leagueId);
        leagueProgressValidator.validateMemberCount(
                teamMemberReader.countMembers(teamId), minTeamMembers);

        long entryFee = leagueReader.getEntryFee(leagueId);
        leagueProgressValidator.validateSufficientTreasury(teamReader.getTreasury(teamId), entryFee);

        teamLeagueProgressExecutor.enterLowestTier(teamId, format);
        return teamLeagueProgressReader.getProgressResponse(teamId, format);
    }

    /**
     * 상위 티어로 승급한다. 상한 도달 + 참가비 지불 필요.
     *
     * @param userId 유저 ID
     * @param format 포맷
     * @param targetTier 목표 티어
     * @return 승급 후 진행 상태
     */
    public TeamLeagueProgressResponse promote(Long userId, LeagueFormat format, LeagueTier targetTier) {
        Long teamId = requireTeamId(userId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, userId));

        TeamLeagueProgressResponse current = teamLeagueProgressReader.getProgressResponse(teamId, format);
        leagueProgressValidator.validatePromote(
                current.promoteReady(), current.currentTier(), targetTier);

        Long leagueId = leagueReader.getByFormatAndTier(format, targetTier).leagueId();
        int minTeamMembers = leagueReader.getMinTeamMembers(leagueId);
        leagueProgressValidator.validateMemberCount(
                teamMemberReader.countMembers(teamId), minTeamMembers);

        long entryFee = leagueReader.getEntryFee(leagueId);
        leagueProgressValidator.validateSufficientTreasury(teamReader.getTreasury(teamId), entryFee);

        teamLeagueProgressExecutor.promote(teamId, format, targetTier);
        return teamLeagueProgressReader.getProgressResponse(teamId, format);
    }

    /**
     * 유저의 소속 팀 ID를 조회한다.
     *
     * @param userId 유저 ID
     * @return 팀 ID
     */
    private Long requireTeamId(Long userId) {
        return teamMemberReader.findTeamIdByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEAM_NOT_FOUND, "userId=" + userId));
    }
}
