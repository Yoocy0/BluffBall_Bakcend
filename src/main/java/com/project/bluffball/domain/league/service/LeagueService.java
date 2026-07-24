package com.project.bluffball.domain.league.service;

import com.project.bluffball.domain.league.dto.response.LeagueEntryTicketResponse;
import com.project.bluffball.domain.league.dto.response.LeaguePrizeResponse;
import com.project.bluffball.domain.league.dto.response.LeagueResponse;
import com.project.bluffball.domain.league.dto.response.LeagueSeasonResponse;
import com.project.bluffball.domain.league.dto.response.LeagueStandingItemResponse;
import com.project.bluffball.domain.league.dto.response.MyLeagueResponse;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueSeasonStatus;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.service.usecase.executor.LeagueEntryTicketExecutor;
import com.project.bluffball.domain.league.service.usecase.executor.LeagueJoinExecutor;
import com.project.bluffball.domain.league.service.usecase.reader.LeaguePrizeReader;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueReader;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueSeasonReader;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueTeamReader;
import com.project.bluffball.domain.league.service.usecase.validator.LeagueJoinValidator;
import com.project.bluffball.domain.league.service.usecase.validator.LeagueTicketValidator;
import com.project.bluffball.domain.team.service.usecase.reader.TeamMemberReader;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import com.project.bluffball.domain.team.service.usecase.validator.TeamMembershipValidator;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 리그 서비스.
 *
 * <p>리그 카탈로그·시즌·참여권·참가·순위·상금 API 유스케이스를 조립한다.
 * Entity·Repository에 직접 접근하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class LeagueService {

    private final LeagueTicketValidator leagueTicketValidator;
    private final LeagueJoinValidator leagueJoinValidator;
    private final TeamMembershipValidator teamMembershipValidator;

    private final LeagueReader leagueReader;
    private final LeagueSeasonReader leagueSeasonReader;
    private final LeagueTeamReader leagueTeamReader;
    private final LeaguePrizeReader leaguePrizeReader;
    private final TeamMemberReader teamMemberReader;
    private final TeamReader teamReader;

    private final LeagueEntryTicketExecutor leagueEntryTicketExecutor;
    private final LeagueJoinExecutor leagueJoinExecutor;

    /**
     * 리그 카탈로그 목록을 조회한다.
     *
     * @param format 포맷 필터
     * @param tier 티어 필터
     * @return 리그 목록
     */
    public List<LeagueResponse> getLeagues(LeagueFormat format, LeagueTier tier) {
        return leagueReader.getLeagues(format, tier);
    }

    /**
     * 리그 등급 상세를 조회한다.
     *
     * @param leagueId 리그 ID
     * @return 리그 상세
     */
    public LeagueResponse getLeague(Long leagueId) {
        return leagueReader.getLeagueResponse(leagueId);
    }

    /**
     * 내 팀의 현재 소속 리그를 조회한다.
     *
     * @param userId 유저 ID
     * @return 소속 리그 정보
     */
    public MyLeagueResponse getMyLeague(Long userId) {
        Long teamId = requireTeamId(userId);
        return leagueTeamReader.getMyLeague(teamId);
    }

    /**
     * 시즌 목록을 조회한다.
     *
     * @param format 포맷 필터
     * @param tier 티어 필터
     * @param status 상태 필터
     * @return 시즌 목록
     */
    public List<LeagueSeasonResponse> getSeasons(
            LeagueFormat format,
            LeagueTier tier,
            LeagueSeasonStatus status) {
        return leagueSeasonReader.getSeasons(format, tier, status);
    }

    /**
     * 시즌 상세를 조회한다.
     *
     * @param seasonId 시즌 ID
     * @return 시즌 상세
     */
    public LeagueSeasonResponse getSeason(Long seasonId) {
        return leagueSeasonReader.getSeasonResponse(seasonId);
    }

    /**
     * 시즌 순위표를 조회한다.
     *
     * @param seasonId 시즌 ID
     * @return 순위 목록
     */
    public List<LeagueStandingItemResponse> getStandings(Long seasonId) {
        return leagueTeamReader.getStandings(seasonId);
    }

    /**
     * 시즌 상금표를 조회한다.
     *
     * @param seasonId 시즌 ID
     * @return 상금표
     */
    public LeaguePrizeResponse getPrizes(Long seasonId) {
        return leaguePrizeReader.getPrizes(seasonId);
    }

    /**
     * 팀 재정으로 시즌 참여권을 구매한다. 리더만 가능하다.
     *
     * @param userId 요청 유저 ID
     * @param seasonId 시즌 ID
     * @return 구매된 참여권 정보
     */
    public LeagueEntryTicketResponse purchaseTicket(Long userId, Long seasonId) {
        Long teamId = requireTeamId(userId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, userId));

        LeagueSeasonStatus status = leagueSeasonReader.getStatus(seasonId);
        leagueTicketValidator.validateRecruiting(status);
        leagueTicketValidator.validateNotAlreadyOwned(leagueTeamReader.hasTicket(teamId, seasonId));

        Long leagueId = leagueSeasonReader.getLeagueId(seasonId);
        long entryFee = leagueReader.getEntryFee(leagueId);
        long treasury = teamReader.getTreasury(teamId);
        leagueTicketValidator.validateSufficientTreasury(treasury, entryFee);

        Long ticketId = leagueEntryTicketExecutor.purchase(teamId, seasonId, userId);
        return leagueTeamReader.getTicketResponse(ticketId);
    }

    /**
     * 참여권과 최소 인원을 검증한 뒤 시즌에 참가한다. 리더만 가능하다.
     *
     * @param userId 요청 유저 ID
     * @param seasonId 시즌 ID
     * @return 참가 후 소속 리그 정보
     */
    public MyLeagueResponse joinSeason(Long userId, Long seasonId) {
        Long teamId = requireTeamId(userId);
        teamMembershipValidator.validateLeader(teamMemberReader.isLeader(teamId, userId));

        LeagueSeasonStatus status = leagueSeasonReader.getStatus(seasonId);
        leagueJoinValidator.validateRecruiting(status);
        leagueJoinValidator.validateTicketOwned(leagueTeamReader.hasTicket(teamId, seasonId));
        leagueJoinValidator.validateNotAlreadyJoined(leagueTeamReader.isJoined(teamId, seasonId));

        Long leagueId = leagueSeasonReader.getLeagueId(seasonId);
        int minTeamMembers = leagueReader.getMinTeamMembers(leagueId);
        long memberCount = teamMemberReader.countMembers(teamId);
        leagueJoinValidator.validateMemberCount(memberCount, minTeamMembers);

        long joinedTeamCount = leagueSeasonReader.getJoinedTeamCount(seasonId);
        int maxTeams = leagueSeasonReader.getMaxTeams(seasonId);
        leagueJoinValidator.validateCapacity(joinedTeamCount, maxTeams);

        Long joinedSeasonId = leagueJoinExecutor.join(teamId, seasonId);
        return leagueTeamReader.getAffiliation(teamId, joinedSeasonId);
    }

    /**
     * 유저의 소속 팀 ID를 조회한다.
     *
     * @param userId 유저 ID
     * @return 팀 ID
     * @throws NotFoundException 소속 팀이 없으면
     */
    private Long requireTeamId(Long userId) {
        return teamMemberReader.findTeamIdByUserId(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEAM_NOT_FOUND, "userId=" + userId));
    }
}
