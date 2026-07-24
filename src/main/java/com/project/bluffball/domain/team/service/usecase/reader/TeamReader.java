package com.project.bluffball.domain.team.service.usecase.reader;

import com.project.bluffball.domain.league.entity.LeagueSeason;
import com.project.bluffball.domain.league.entity.LeagueTeam;
import com.project.bluffball.domain.league.enums.LeagueSeasonStatus;
import com.project.bluffball.domain.league.repository.LeagueSeasonRepository;
import com.project.bluffball.domain.league.repository.LeagueTeamRepository;
import com.project.bluffball.domain.team.dto.response.TeamResponse;
import com.project.bluffball.domain.team.entity.Team;
import com.project.bluffball.domain.team.repository.TeamRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 팀 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamReader {

    /** 현재 소속 리그로 인정하는 시즌 상태 */
    private static final Set<LeagueSeasonStatus> ACTIVE_SEASON_STATUSES =
            EnumSet.of(LeagueSeasonStatus.RECRUITING, LeagueSeasonStatus.IN_PROGRESS);

    private final TeamRepository teamRepository;
    private final LeagueTeamRepository leagueTeamRepository;
    private final LeagueSeasonRepository leagueSeasonRepository;

    /**
     * 팀 Entity를 조회한다. Executor·Reader 내부 전용 (Service에서 호출 금지).
     *
     * @param teamId 팀 ID
     * @return 팀 Entity
     * @throws NotFoundException 팀이 없으면
     */
    public Team getById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TEAM_NOT_FOUND, "teamId=" + teamId));
    }

    /**
     * 팀 이름 존재 여부를 반환한다.
     *
     * @param name 팀 이름
     * @return 존재하면 true
     */
    public boolean existsByName(String name) {
        return teamRepository.existsByName(name);
    }

    /**
     * 팀 존재 여부를 반환한다.
     *
     * @param teamId 팀 ID
     * @return 존재하면 true
     */
    public boolean exists(Long teamId) {
        return teamRepository.existsById(teamId);
    }

    /**
     * 팀 상세 DTO를 반환한다.
     *
     * @param teamId 팀 ID
     * @return 팀 응답 DTO
     */
    public TeamResponse getTeamResponse(Long teamId) {
        Team team = getById(teamId);
        return toResponse(team);
    }

    /**
     * 팀 이름 부분 일치 검색 결과를 반환한다.
     *
     * @param name 검색어 (null이면 전체)
     * @return 팀 응답 목록
     */
    public List<TeamResponse> search(String name) {
        List<Team> teams = (name == null || name.isBlank())
                ? teamRepository.findAll()
                : teamRepository.findByNameContainingIgnoreCase(name.trim());
        return teams.stream().map(this::toResponse).toList();
    }

    /**
     * 팀 재정 잔액을 반환한다.
     *
     * @param teamId 팀 ID
     * @return 재정 잔액
     */
    public long getTreasury(Long teamId) {
        return getById(teamId).getTreasury();
    }

    /**
     * Entity → 응답 DTO 변환.
     *
     * @param team 팀 Entity
     * @return 응답 DTO
     */
    private TeamResponse toResponse(Team team) {
        Optional<CurrentLeagueIds> current = findCurrentLeagueIds(team.getId());
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getLogoUrl(),
                team.getLeaderUserId(),
                team.getTreasury(),
                current.map(CurrentLeagueIds::seasonId).orElse(null),
                current.map(CurrentLeagueIds::leagueId).orElse(null));
    }

    /**
     * 진행·모집 중인 시즌 소속 정보를 조회한다.
     *
     * @param teamId 팀 ID
     * @return 현재 시즌·리그 ID
     */
    private Optional<CurrentLeagueIds> findCurrentLeagueIds(Long teamId) {
        List<LeagueTeam> leagueTeams = leagueTeamRepository.findByTeamId(teamId);
        for (LeagueTeam leagueTeam : leagueTeams) {
            Optional<LeagueSeason> seasonOpt = leagueSeasonRepository.findById(leagueTeam.getLeagueSeasonId());
            if (seasonOpt.isEmpty()) {
                continue;
            }
            LeagueSeason season = seasonOpt.get();
            if (ACTIVE_SEASON_STATUSES.contains(season.getStatus())) {
                return Optional.of(new CurrentLeagueIds(season.getId(), season.getLeagueId()));
            }
        }
        return Optional.empty();
    }

    /** 현재 소속 리그 식별값 묶음 */
    private record CurrentLeagueIds(Long seasonId, Long leagueId) {
    }
}
