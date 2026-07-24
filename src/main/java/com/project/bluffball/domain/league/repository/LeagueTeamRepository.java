package com.project.bluffball.domain.league.repository;

import com.project.bluffball.domain.league.entity.LeagueTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 리그 시즌 소속 팀 JPA Repository.
 */
public interface LeagueTeamRepository extends JpaRepository<LeagueTeam, Long> {

    /**
     * 팀의 전체 리그 참가 기록을 조회한다.
     *
     * @param teamId 팀 ID
     * @return 리그 팀 기록 목록
     */
    List<LeagueTeam> findByTeamId(Long teamId);

    /**
     * 팀·시즌 조합으로 참가 기록을 조회한다.
     *
     * @param teamId 팀 ID
     * @param leagueSeasonId 시즌 ID
     * @return 리그 팀 기록 Optional
     */
    Optional<LeagueTeam> findByTeamIdAndLeagueSeasonId(Long teamId, Long leagueSeasonId);

    /**
     * 시즌의 참가 팀을 점수 내림차순으로 조회한다.
     *
     * @param leagueSeasonId 시즌 ID
     * @return 순위용 팀 목록
     */
    List<LeagueTeam> findByLeagueSeasonIdOrderByScoreDesc(Long leagueSeasonId);

    /**
     * 시즌 참가 팀 수를 반환한다.
     *
     * @param leagueSeasonId 시즌 ID
     * @return 참가 팀 수
     */
    long countByLeagueSeasonId(Long leagueSeasonId);
}
