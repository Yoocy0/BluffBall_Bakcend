package com.project.bluffball.domain.league.repository;

import com.project.bluffball.domain.league.entity.TeamLeagueProgress;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 팀 리그 진행 상태 Repository.
 */
public interface TeamLeagueProgressRepository extends JpaRepository<TeamLeagueProgress, Long> {

    /**
     * 팀·포맷 진행 상태를 조회한다.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @return Optional
     */
    Optional<TeamLeagueProgress> findByTeamIdAndFormat(Long teamId, LeagueFormat format);

    /**
     * 팀의 전 포맷 진행 상태를 조회한다.
     *
     * @param teamId 팀 ID
     * @return 목록
     */
    List<TeamLeagueProgress> findByTeamId(Long teamId);

    /**
     * 포맷·티어 소속 팀을 조회한다 (강등 배치용).
     *
     * @param format 포맷
     * @param currentTier 티어
     * @return 목록
     */
    List<TeamLeagueProgress> findByFormatAndCurrentTier(LeagueFormat format, LeagueTier currentTier);

    /**
     * 포맷·티어 소속 팀을 순위 기준으로 조회한다.
     *
     * <p>정렬: rating ↓ → wins ↓ → runDiff ↓ → teamId ↑</p>
     *
     * @param format 포맷
     * @param currentTier 티어
     * @return 순위 정렬된 진행 상태 목록
     */
    List<TeamLeagueProgress> findByFormatAndCurrentTierOrderByRatingDescWinsDescRunDiffDescTeamIdAsc(
            LeagueFormat format,
            LeagueTier currentTier);

    /**
     * 존재 여부.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @return 있으면 true
     */
    boolean existsByTeamIdAndFormat(Long teamId, LeagueFormat format);
}
