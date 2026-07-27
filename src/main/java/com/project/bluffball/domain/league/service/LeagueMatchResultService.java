package com.project.bluffball.domain.league.service;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.service.usecase.executor.LeagueMatchResultExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 리그 경기 결과 반영 서비스.
 *
 * <p>점수·전적·매치 보상을 상시 티어 진행 상태에 반영한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeagueMatchResultService {

    private final LeagueMatchResultExecutor leagueMatchResultExecutor;

    /**
     * 경기 결과를 티어 점수·금고 보상에 반영한다.
     *
     * @param format 포맷
     * @param homeTeamId 홈 팀 ID
     * @param awayTeamId 어웨이 팀 ID
     * @param homeScore 홈 득점
     * @param awayScore 어웨이 득점
     */
    public void applyMatchResult(
            LeagueFormat format,
            Long homeTeamId,
            Long awayTeamId,
            int homeScore,
            int awayScore) {
        leagueMatchResultExecutor.applyMatchResult(
                format, homeTeamId, awayTeamId, homeScore, awayScore);
        log.info("리그 결과 반영 format={} homeTeamId={} awayTeamId={} score={}:{}",
                format, homeTeamId, awayTeamId, homeScore, awayScore);
    }
}
