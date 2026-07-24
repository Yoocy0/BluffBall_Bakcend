package com.project.bluffball.domain.league.service.usecase.executor;

import com.project.bluffball.domain.league.entity.LeagueEntryTicket;
import com.project.bluffball.domain.league.entity.LeagueTeam;
import com.project.bluffball.domain.league.repository.LeagueEntryTicketRepository;
import com.project.bluffball.domain.league.repository.LeagueTeamRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리그 시즌 참가 쓰기 전담 Executor (usecase/executor 계층).
 */
@Component
@RequiredArgsConstructor
public class LeagueJoinExecutor {

    private final LeagueEntryTicketRepository leagueEntryTicketRepository;
    private final LeagueTeamRepository leagueTeamRepository;

    /**
     * 참여권을 사용해 시즌에 팀을 등록한다.
     *
     * @param teamId 팀 ID
     * @param seasonId 시즌 ID
     * @return 시즌 ID
     */
    @Transactional
    public Long join(Long teamId, Long seasonId) {
        LeagueEntryTicket ticket = leagueEntryTicketRepository
                .findByTeamIdAndLeagueSeasonId(teamId, seasonId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.LEAGUE_TICKET_NOT_FOUND,
                        "teamId=" + teamId + ", seasonId=" + seasonId));

        leagueTeamRepository.save(new LeagueTeam(seasonId, teamId, ticket.getId()));
        return seasonId;
    }
}
