package com.project.bluffball.domain.league.service.usecase.executor;

import com.project.bluffball.domain.league.entity.LeagueEntryTicket;
import com.project.bluffball.domain.league.repository.LeagueEntryTicketRepository;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueReader;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueSeasonReader;
import com.project.bluffball.domain.team.entity.Team;
import com.project.bluffball.domain.team.entity.TeamTreasuryTransaction;
import com.project.bluffball.domain.team.repository.TeamRepository;
import com.project.bluffball.domain.team.repository.TeamTreasuryTransactionRepository;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리그 참여권 구매 쓰기 전담 Executor (usecase/executor 계층).
 */
@Component
@RequiredArgsConstructor
public class LeagueEntryTicketExecutor {

    private final LeagueSeasonReader leagueSeasonReader;
    private final LeagueReader leagueReader;
    private final TeamReader teamReader;
    private final TeamRepository teamRepository;
    private final TeamTreasuryTransactionRepository teamTreasuryTransactionRepository;
    private final LeagueEntryTicketRepository leagueEntryTicketRepository;

    /**
     * 팀 재정으로 시즌 참여권을 구매한다.
     *
     * @param teamId 팀 ID
     * @param seasonId 시즌 ID
     * @param purchasedByUserId 구매 승인자(리더) ID
     * @return 생성된 참여권 ID
     */
    @Transactional
    public Long purchase(Long teamId, Long seasonId, Long purchasedByUserId) {
        Long leagueId = leagueSeasonReader.getLeagueId(seasonId);
        long entryFee = leagueReader.getEntryFee(leagueId);

        Team team = teamReader.getById(teamId);
        team.payEntryFee(entryFee);
        teamRepository.save(team);

        teamTreasuryTransactionRepository.save(
                TeamTreasuryTransaction.entryFeePayment(teamId, entryFee, seasonId));

        LeagueEntryTicket ticket = leagueEntryTicketRepository.save(
                new LeagueEntryTicket(teamId, seasonId, entryFee, purchasedByUserId));
        return ticket.getId();
    }
}
