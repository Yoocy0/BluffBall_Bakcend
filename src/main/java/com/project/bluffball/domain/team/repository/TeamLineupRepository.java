package com.project.bluffball.domain.team.repository;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.team.entity.TeamLineup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 팀 출전 로스터 JPA Repository.
 */
public interface TeamLineupRepository extends JpaRepository<TeamLineup, Long> {

    /**
     * 팀·포맷 로스터를 조회한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 로스터 Optional
     */
    Optional<TeamLineup> findByTeamIdAndFormat(Long teamId, LeagueFormat format);

    /**
     * 팀·포맷 로스터 존재 여부를 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 존재하면 true
     */
    boolean existsByTeamIdAndFormat(Long teamId, LeagueFormat format);
}
