package com.project.bluffball.domain.league.service.usecase.executor;

import com.project.bluffball.domain.league.LeagueScoreConstants;
import com.project.bluffball.domain.league.config.LeagueTierRule;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
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
 * 리그 경기 종료 시 점수·재화 반영 Executor.
 */
@Component
@RequiredArgsConstructor
public class LeagueMatchResultExecutor {

    private final TeamLeagueProgressExecutor teamLeagueProgressExecutor;
    private final TeamLeagueProgressReader teamLeagueProgressReader;
    private final TeamReader teamReader;
    private final TeamRepository teamRepository;
    private final TeamTreasuryTransactionRepository teamTreasuryTransactionRepository;

    /**
     * 양 팀에 점수·전적·매치 보상을 반영한다.
     *
     * @param format 포맷
     * @param homeTeamId 홈 팀
     * @param awayTeamId 어웨이 팀
     * @param homeScore 홈 득점
     * @param awayScore 어웨이 득점
     */
    @Transactional
    public void applyMatchResult(
            LeagueFormat format,
            Long homeTeamId,
            Long awayTeamId,
            int homeScore,
            int awayScore) {
        if (homeScore == awayScore) {
            return;
        }
        boolean homeWon = homeScore > awayScore;
        int homeDiff = homeScore - awayScore;
        int awayDiff = awayScore - homeScore;

        teamLeagueProgressExecutor.applyMatchResult(homeTeamId, format, homeWon, homeDiff);
        teamLeagueProgressExecutor.applyMatchResult(awayTeamId, format, !homeWon, awayDiff);

        payMatchReward(homeTeamId, format, homeWon);
        payMatchReward(awayTeamId, format, !homeWon);
    }

    /**
     * 매치 보상을 팀 금고에 지급한다.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @param won 승리 여부
     */
    private void payMatchReward(Long teamId, LeagueFormat format, boolean won) {
        LeagueTier tier = teamLeagueProgressReader.getCurrentTier(teamId, format);
        int tierIndex = LeagueTierRule.LADDER.indexOf(tier) + 1;
        long amount = won
                ? LeagueScoreConstants.MATCH_WIN_REWARD_PER_TIER * tierIndex
                : LeagueScoreConstants.MATCH_LOSS_REWARD_PER_TIER * tierIndex;
        if (amount <= 0) {
            return;
        }
        Team team = teamReader.getById(teamId);
        team.addMatchReward(amount);
        teamRepository.save(team);
        teamTreasuryTransactionRepository.save(
                TeamTreasuryTransaction.matchReward(teamId, amount));
    }
}
