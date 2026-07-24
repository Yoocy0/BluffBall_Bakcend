package com.project.bluffball.domain.league.service.usecase.reader;

import com.project.bluffball.domain.league.dto.response.LeagueEntryTicketResponse;
import com.project.bluffball.domain.league.dto.response.LeagueStandingItemResponse;
import com.project.bluffball.domain.league.dto.response.MyLeagueResponse;
import com.project.bluffball.domain.league.entity.League;
import com.project.bluffball.domain.league.entity.LeagueEntryTicket;
import com.project.bluffball.domain.league.entity.LeagueSeason;
import com.project.bluffball.domain.league.entity.LeagueTeam;
import com.project.bluffball.domain.league.enums.LeagueSeasonStatus;
import com.project.bluffball.domain.league.repository.LeagueEntryTicketRepository;
import com.project.bluffball.domain.league.repository.LeagueTeamRepository;
import com.project.bluffball.domain.team.dto.response.TeamResponse;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 리그 시즌 소속 팀·참여권 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class LeagueTeamReader {

    /** 현재 소속 리그로 인정하는 시즌 상태 */
    private static final Set<LeagueSeasonStatus> ACTIVE_SEASON_STATUSES =
            EnumSet.of(LeagueSeasonStatus.RECRUITING, LeagueSeasonStatus.IN_PROGRESS);

    private final LeagueTeamRepository leagueTeamRepository;
    private final LeagueEntryTicketRepository leagueEntryTicketRepository;
    private final LeagueSeasonReader leagueSeasonReader;
    private final LeagueReader leagueReader;
    private final TeamReader teamReader;

    /**
     * 팀·시즌 참가 여부를 반환한다.
     *
     * @param teamId 팀 ID
     * @param seasonId 시즌 ID
     * @return 참가 중이면 true
     */
    public boolean isJoined(Long teamId, Long seasonId) {
        return leagueTeamRepository.findByTeamIdAndLeagueSeasonId(teamId, seasonId).isPresent();
    }

    /**
     * 팀·시즌 참여권 보유 여부를 반환한다.
     *
     * @param teamId 팀 ID
     * @param seasonId 시즌 ID
     * @return 보유하면 true
     */
    public boolean hasTicket(Long teamId, Long seasonId) {
        return leagueEntryTicketRepository.existsByTeamIdAndLeagueSeasonId(teamId, seasonId);
    }

    /**
     * 참여권 응답 DTO를 반환한다.
     *
     * @param ticketId 참여권 ID
     * @return 참여권 응답 DTO
     */
    public LeagueEntryTicketResponse getTicketResponse(Long ticketId) {
        LeagueEntryTicket ticket = leagueEntryTicketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.LEAGUE_TICKET_NOT_FOUND, "ticketId=" + ticketId));
        return new LeagueEntryTicketResponse(
                ticket.getId(),
                ticket.getTeamId(),
                ticket.getLeagueSeasonId(),
                ticket.getFeeAmount());
    }

    /**
     * 시즌 순위표를 반환한다. (점수 내림차순, 표시용 rank는 1부터)
     *
     * @param seasonId 시즌 ID
     * @return 순위 항목 목록
     */
    public List<LeagueStandingItemResponse> getStandings(Long seasonId) {
        // 시즌 존재 확인
        leagueSeasonReader.getById(seasonId);

        List<LeagueTeam> leagueTeams =
                leagueTeamRepository.findByLeagueSeasonIdOrderByScoreDesc(seasonId);
        List<LeagueStandingItemResponse> standings = new ArrayList<>();
        int displayRank = 1;
        for (LeagueTeam leagueTeam : leagueTeams) {
            TeamResponse team = teamReader.getTeamResponse(leagueTeam.getTeamId());
            int rank = leagueTeam.getRank() > 0 ? leagueTeam.getRank() : displayRank;
            standings.add(new LeagueStandingItemResponse(
                    rank,
                    team.teamId(),
                    team.name(),
                    team.logoUrl(),
                    leagueTeam.getWins(),
                    leagueTeam.getLoses(),
                    leagueTeam.getRunDiff(),
                    leagueTeam.getScore()));
            displayRank++;
        }
        return standings;
    }

    /**
     * 팀의 현재 소속 리그 정보를 반환한다.
     *
     * @param teamId 팀 ID
     * @return 내 리그 응답 DTO
     * @throws NotFoundException 활성 소속 리그가 없으면
     */
    public MyLeagueResponse getMyLeague(Long teamId) {
        return findActiveAffiliation(teamId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.LEAGUE_AFFILIATION_NOT_FOUND, "teamId=" + teamId));
    }

    /**
     * 시즌·팀 조합의 소속 정보를 반환한다.
     *
     * @param teamId 팀 ID
     * @param seasonId 시즌 ID
     * @return 내 리그 응답 DTO
     */
    public MyLeagueResponse getAffiliation(Long teamId, Long seasonId) {
        LeagueTeam leagueTeam = leagueTeamRepository.findByTeamIdAndLeagueSeasonId(teamId, seasonId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.LEAGUE_AFFILIATION_NOT_FOUND,
                        "teamId=" + teamId + ", seasonId=" + seasonId));
        return toMyLeague(leagueTeam);
    }

    /**
     * 활성 시즌 소속을 찾는다.
     *
     * @param teamId 팀 ID
     * @return 내 리그 응답 Optional
     */
    private Optional<MyLeagueResponse> findActiveAffiliation(Long teamId) {
        List<LeagueTeam> leagueTeams = leagueTeamRepository.findByTeamId(teamId);
        for (LeagueTeam leagueTeam : leagueTeams) {
            LeagueSeason season = leagueSeasonReader.getById(leagueTeam.getLeagueSeasonId());
            if (ACTIVE_SEASON_STATUSES.contains(season.getStatus())) {
                return Optional.of(toMyLeague(leagueTeam));
            }
        }
        return Optional.empty();
    }

    /**
     * LeagueTeam → MyLeagueResponse 변환.
     *
     * @param leagueTeam 리그 팀 Entity
     * @return 내 리그 응답 DTO
     */
    private MyLeagueResponse toMyLeague(LeagueTeam leagueTeam) {
        LeagueSeason season = leagueSeasonReader.getById(leagueTeam.getLeagueSeasonId());
        League league = leagueReader.getById(season.getLeagueId());
        return new MyLeagueResponse(
                leagueTeam.getTeamId(),
                season.getId(),
                league.getId(),
                league.getFormat(),
                league.getTier(),
                season.getStatus(),
                leagueTeam.getRank(),
                leagueTeam.getWins(),
                leagueTeam.getLoses(),
                leagueTeam.getRunDiff(),
                leagueTeam.getScore());
    }
}
