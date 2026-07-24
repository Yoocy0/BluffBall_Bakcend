package com.project.bluffball.domain.league.repository;

import com.project.bluffball.domain.league.entity.LeagueSeason;
import com.project.bluffball.domain.league.enums.LeagueSeasonStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 리그 시즌 JPA Repository.
 */
public interface LeagueSeasonRepository extends JpaRepository<LeagueSeason, Long> {

    /**
     * 리그 ID로 시즌 목록을 조회한다.
     *
     * @param leagueId 리그 ID
     * @return 시즌 목록
     */
    List<LeagueSeason> findByLeagueId(Long leagueId);

    /**
     * 상태로 시즌 목록을 조회한다.
     *
     * @param status 시즌 상태
     * @return 시즌 목록
     */
    List<LeagueSeason> findByStatus(LeagueSeasonStatus status);
}
