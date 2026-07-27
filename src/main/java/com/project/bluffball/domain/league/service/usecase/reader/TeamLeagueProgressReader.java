package com.project.bluffball.domain.league.service.usecase.reader;

import com.project.bluffball.domain.league.config.LeagueTierRule;
import com.project.bluffball.domain.league.dto.response.TeamLeagueProgressResponse;
import com.project.bluffball.domain.league.entity.TeamLeagueProgress;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.repository.TeamLeagueProgressRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 팀 리그 진행 상태 Reader.
 */
@Component
@RequiredArgsConstructor
public class TeamLeagueProgressReader {

    private final TeamLeagueProgressRepository teamLeagueProgressRepository;

    /**
     * Entity 조회 (Executor·Reader 내부용).
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @return Entity
     */
    public TeamLeagueProgress getByTeamIdAndFormat(Long teamId, LeagueFormat format) {
        return teamLeagueProgressRepository.findByTeamIdAndFormat(teamId, format)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.LEAGUE_AFFILIATION_NOT_FOUND,
                        "teamId=" + teamId + ", format=" + format));
    }

    /**
     * 진행 상태 존재 여부.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @return 있으면 true
     */
    public boolean exists(Long teamId, LeagueFormat format) {
        return teamLeagueProgressRepository.existsByTeamIdAndFormat(teamId, format);
    }

    /**
     * 현재 티어.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @return 티어
     */
    public LeagueTier getCurrentTier(Long teamId, LeagueFormat format) {
        return getByTeamIdAndFormat(teamId, format).getCurrentTier();
    }

    /**
     * 요청 티어와 현재 티어가 같은지.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @param tier 요청 티어
     * @return 일치하면 true
     */
    public boolean isCurrentTier(Long teamId, LeagueFormat format, LeagueTier tier) {
        return exists(teamId, format) && getCurrentTier(teamId, format) == tier;
    }

    /**
     * 응답 DTO.
     *
     * @param teamId 팀 ID
     * @param format 포맷
     * @return 응답
     */
    public TeamLeagueProgressResponse getProgressResponse(Long teamId, LeagueFormat format) {
        return toResponse(getByTeamIdAndFormat(teamId, format));
    }

    /**
     * 팀의 전 포맷 진행 상태.
     *
     * @param teamId 팀 ID
     * @return 목록
     */
    public List<TeamLeagueProgressResponse> getProgressResponses(Long teamId) {
        return teamLeagueProgressRepository.findByTeamId(teamId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Entity → DTO.
     *
     * @param progress Entity
     * @return DTO
     */
    private TeamLeagueProgressResponse toResponse(TeamLeagueProgress progress) {
        LeagueTierRule rule = LeagueTierRule.of(progress.getCurrentTier());
        return new TeamLeagueProgressResponse(
                progress.getTeamId(),
                progress.getFormat(),
                progress.getCurrentTier(),
                progress.getRating(),
                rule.floor(),
                rule.ceil(),
                progress.isPromoteReady(),
                LeagueTierRule.nextTier(progress.getCurrentTier()),
                progress.getWins(),
                progress.getLoses(),
                progress.getRunDiff());
    }
}
