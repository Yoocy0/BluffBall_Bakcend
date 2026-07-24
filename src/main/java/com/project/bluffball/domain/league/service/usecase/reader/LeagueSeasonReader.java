package com.project.bluffball.domain.league.service.usecase.reader;

import com.project.bluffball.domain.league.dto.response.LeagueSeasonResponse;
import com.project.bluffball.domain.league.entity.League;
import com.project.bluffball.domain.league.entity.LeagueSeason;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueSeasonStatus;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.repository.LeagueSeasonRepository;
import com.project.bluffball.domain.league.repository.LeagueTeamRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 리그 시즌 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class LeagueSeasonReader {

    private final LeagueSeasonRepository leagueSeasonRepository;
    private final LeagueTeamRepository leagueTeamRepository;
    private final LeagueReader leagueReader;

    /**
     * 시즌 Entity를 조회한다. Executor·Reader 내부 전용 (Service에서 호출 금지).
     *
     * @param seasonId 시즌 ID
     * @return 시즌 Entity
     * @throws NotFoundException 시즌이 없으면
     */
    public LeagueSeason getById(Long seasonId) {
        return leagueSeasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.LEAGUE_SEASON_NOT_FOUND, "seasonId=" + seasonId));
    }

    /**
     * 시즌 상세 DTO를 반환한다.
     *
     * @param seasonId 시즌 ID
     * @return 시즌 응답 DTO
     */
    public LeagueSeasonResponse getSeasonResponse(Long seasonId) {
        return toResponse(getById(seasonId));
    }

    /**
     * 시즌 상태를 반환한다.
     *
     * @param seasonId 시즌 ID
     * @return 시즌 상태
     */
    public LeagueSeasonStatus getStatus(Long seasonId) {
        return getById(seasonId).getStatus();
    }

    /**
     * 시즌의 소속 리그 ID를 반환한다.
     *
     * @param seasonId 시즌 ID
     * @return 리그 ID
     */
    public Long getLeagueId(Long seasonId) {
        return getById(seasonId).getLeagueId();
    }

    /**
     * 시즌 최대 팀 수를 반환한다.
     *
     * @param seasonId 시즌 ID
     * @return 최대 팀 수
     */
    public int getMaxTeams(Long seasonId) {
        return getById(seasonId).getMaxTeams();
    }

    /**
     * 시즌 참가 팀 수를 반환한다.
     *
     * @param seasonId 시즌 ID
     * @return 참가 팀 수
     */
    public long getJoinedTeamCount(Long seasonId) {
        return leagueTeamRepository.countByLeagueSeasonId(seasonId);
    }

    /**
     * 필터 조건에 맞는 시즌 목록을 반환한다.
     *
     * @param format 포맷 필터 (nullable)
     * @param tier 티어 필터 (nullable)
     * @param status 상태 필터 (nullable)
     * @return 시즌 응답 목록
     */
    public List<LeagueSeasonResponse> getSeasons(
            LeagueFormat format,
            LeagueTier tier,
            LeagueSeasonStatus status) {

        List<LeagueSeason> seasons = (status != null)
                ? leagueSeasonRepository.findByStatus(status)
                : leagueSeasonRepository.findAll();

        return seasons.stream()
                .filter(season -> matchesLeagueFilter(season.getLeagueId(), format, tier))
                .map(this::toResponse)
                .toList();
    }

    /**
     * 리그 포맷·티어 필터 일치 여부를 판정한다.
     *
     * @param leagueId 리그 ID
     * @param format 포맷 필터
     * @param tier 티어 필터
     * @return 일치하면 true
     */
    private boolean matchesLeagueFilter(Long leagueId, LeagueFormat format, LeagueTier tier) {
        if (format == null && tier == null) {
            return true;
        }
        League league = leagueReader.getById(leagueId);
        if (format != null && league.getFormat() != format) {
            return false;
        }
        return tier == null || league.getTier() == tier;
    }

    /**
     * Entity → 응답 DTO 변환.
     *
     * @param season 시즌 Entity
     * @return 응답 DTO
     */
    private LeagueSeasonResponse toResponse(LeagueSeason season) {
        League league = leagueReader.getById(season.getLeagueId());
        long joinedTeamCount = leagueTeamRepository.countByLeagueSeasonId(season.getId());
        return new LeagueSeasonResponse(
                season.getId(),
                season.getLeagueId(),
                league.getFormat(),
                league.getTier(),
                season.getPeriodLabel(),
                season.getGroupNumber(),
                season.getStatus(),
                season.getStartAt(),
                season.getEndAt(),
                season.getMaxTeams(),
                (int) joinedTeamCount);
    }
}
