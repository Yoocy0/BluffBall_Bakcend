package com.project.bluffball.domain.league.service.usecase.executor;

import com.project.bluffball.domain.league.config.LeagueTierRule;
import com.project.bluffball.domain.league.entity.TeamLeagueProgress;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.repository.TeamLeagueProgressRepository;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueReader;
import com.project.bluffball.domain.league.service.usecase.reader.TeamLeagueProgressReader;
import com.project.bluffball.domain.team.entity.Team;
import com.project.bluffball.domain.team.entity.TeamTreasuryTransaction;
import com.project.bluffball.domain.team.repository.TeamRepository;
import com.project.bluffball.domain.team.repository.TeamTreasuryTransactionRepository;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 티어 진입·승급·점수 반영 Executor.
 */
@Component
@RequiredArgsConstructor
public class TeamLeagueProgressExecutor {

    private final TeamLeagueProgressRepository teamLeagueProgressRepository;
    private final TeamLeagueProgressReader teamLeagueProgressReader;
    private final LeagueReader leagueReader;
    private final TeamReader teamReader;
    private final TeamRepository teamRepository;
    private final TeamTreasuryTransactionRepository teamTreasuryTransactionRepository;

    /**
     * 최하위 티어로 최초 진입하고 참가비를 지불한다.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @return 팀 ID
     */
    @Transactional
    public Long enterLowestTier(Long teamId, LeagueFormat format) {
        LeagueTier tier = LeagueTierRule.lowestTier();
        payEntryFee(teamId, format, tier);
        teamLeagueProgressRepository.save(new TeamLeagueProgress(teamId, format));
        return teamId;
    }

    /**
     * 상위 티어로 승급하고 참가비를 지불한다.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @param targetTier 목표 티어
     * @return 팀 ID
     */
    @Transactional
    public Long promote(Long teamId, LeagueFormat format, LeagueTier targetTier) {
        payEntryFee(teamId, format, targetTier);
        TeamLeagueProgress progress = teamLeagueProgressReader.getByTeamIdAndFormat(teamId, format);
        progress.promoteTo(targetTier);
        teamLeagueProgressRepository.save(progress);
        return teamId;
    }

    /**
     * 경기 결과를 점수·전적에 반영한다.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @param won 승리
     * @param matchRunDiff 득실
     */
    @Transactional
    public void applyMatchResult(Long teamId, LeagueFormat format, boolean won, int matchRunDiff) {
        TeamLeagueProgress progress = teamLeagueProgressReader.getByTeamIdAndFormat(teamId, format);
        progress.applyMatchResult(won, matchRunDiff);
        teamLeagueProgressRepository.save(progress);
    }

    /**
     * 강등한다.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @param previousTier 하위 티어
     */
    @Transactional
    public void demote(Long teamId, LeagueFormat format, LeagueTier previousTier) {
        TeamLeagueProgress progress = teamLeagueProgressReader.getByTeamIdAndFormat(teamId, format);
        progress.demoteTo(previousTier);
        teamLeagueProgressRepository.save(progress);
    }

    /**
     * 참가비를 팀 금고에서 차감한다.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @param tier 티어
     */
    private void payEntryFee(Long teamId, LeagueFormat format, LeagueTier tier) {
        Long leagueId = leagueReader.getByFormatAndTier(format, tier).leagueId();
        long entryFee = leagueReader.getEntryFee(leagueId);
        Team team = teamReader.getById(teamId);
        team.payEntryFee(entryFee);
        teamRepository.save(team);
        teamTreasuryTransactionRepository.save(
                TeamTreasuryTransaction.entryFeePayment(teamId, entryFee));
    }
}
