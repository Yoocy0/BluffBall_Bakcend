package com.project.bluffball.domain.team.repository;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.team.entity.TeamPitchLoadout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 팀 구종 사전 선택 JPA Repository.
 */
public interface TeamPitchLoadoutRepository extends JpaRepository<TeamPitchLoadout, Long> {

    /**
     * 팀·포맷의 전체 사전 선택을 조회한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 사전 선택 목록
     */
    List<TeamPitchLoadout> findByTeamIdAndFormat(Long teamId, LeagueFormat format);

    /**
     * 팀·포맷·유저 사전 선택을 조회한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param userId 유저 ID
     * @return 사전 선택 Optional
     */
    Optional<TeamPitchLoadout> findByTeamIdAndFormatAndUserId(
            Long teamId, LeagueFormat format, Long userId);

    /**
     * 팀·포맷의 사전 선택 개수를 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 개수
     */
    long countByTeamIdAndFormat(Long teamId, LeagueFormat format);

    /**
     * 팀·포맷의 사전 선택을 모두 삭제한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     */
    void deleteByTeamIdAndFormat(Long teamId, LeagueFormat format);

    /**
     * 팀·포맷에서 특정 유저들의 사전 선택을 삭제한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param userIds 유저 ID 목록
     */
    void deleteByTeamIdAndFormatAndUserIdIn(Long teamId, LeagueFormat format, List<Long> userIds);
}
