package com.project.bluffball.domain.league.repository;

import com.project.bluffball.domain.league.entity.League;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 리그 카탈로그 JPA Repository.
 */
public interface LeagueRepository extends JpaRepository<League, Long> {

    /**
     * 포맷·티어로 리그를 조회한다.
     *
     * @param format 리그 구분
     * @param tier 리그 단계
     * @return 리그 Optional
     */
    Optional<League> findByFormatAndTier(LeagueFormat format, LeagueTier tier);

    /**
     * 포맷으로 리그 목록을 조회한다.
     *
     * @param format 리그 구분
     * @return 리그 목록
     */
    List<League> findByFormat(LeagueFormat format);

    /**
     * 티어로 리그 목록을 조회한다.
     *
     * @param tier 리그 단계
     * @return 리그 목록
     */
    List<League> findByTier(LeagueTier tier);
}
