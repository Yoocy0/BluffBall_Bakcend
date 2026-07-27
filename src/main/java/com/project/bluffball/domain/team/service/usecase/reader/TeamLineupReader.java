package com.project.bluffball.domain.team.service.usecase.reader;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.team.dto.response.TeamLineupResponse;
import com.project.bluffball.domain.team.entity.TeamLineup;
import com.project.bluffball.domain.team.repository.TeamLineupRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 출전 로스터 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamLineupReader {

    private final TeamLineupRepository teamLineupRepository;

    /**
     * 타순 + 선발 투수를 출전 전원 목록으로 합친다.
     *
     * <p>Compact처럼 투수가 타순 밖이면 맨 뒤에 추가한다.</p>
     *
     * @param battingOrder 타순
     * @param startingPitcherUserId 선발 투수
     * @return 출전 전원
     */
    public static List<Long> mergeMatchRoster(List<Long> battingOrder, Long startingPitcherUserId) {
        List<Long> roster = new ArrayList<>(battingOrder);
        if (startingPitcherUserId != null && !roster.contains(startingPitcherUserId)) {
            roster.add(startingPitcherUserId);
        }
        return roster;
    }

    /**
     * 로스터 Entity를 조회한다. Executor·Reader 내부 전용 (Service에서 호출 금지).
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 로스터 Entity
     * @throws NotFoundException 로스터가 없으면
     */
    public TeamLineup getByTeamIdAndFormat(Long teamId, LeagueFormat format) {
        return teamLineupRepository.findByTeamIdAndFormat(teamId, format)
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.TEAM_LINEUP_NOT_FOUND,
                        "teamId=" + teamId + ", format=" + format));
    }

    /**
     * 로스터 존재 여부를 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 존재하면 true
     */
    public boolean exists(Long teamId, LeagueFormat format) {
        return teamLineupRepository.existsByTeamIdAndFormat(teamId, format);
    }

    /**
     * 타순(타자 유저 ID)을 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 타순
     */
    public List<Long> getUserIds(Long teamId, LeagueFormat format) {
        return List.copyOf(getByTeamIdAndFormat(teamId, format).getUserIds());
    }

    /**
     * 출전 전원(타순 + Compact 전담 투수)을 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 출전 전원
     */
    public List<Long> getMatchRosterUserIds(Long teamId, LeagueFormat format) {
        TeamLineup lineup = getByTeamIdAndFormat(teamId, format);
        return List.copyOf(mergeMatchRoster(lineup.getUserIds(), lineup.getStartingPitcherUserId()));
    }

    /**
     * 선발 투수 유저 ID를 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 선발 투수 ID
     */
    public Long getStartingPitcherUserId(Long teamId, LeagueFormat format) {
        return getByTeamIdAndFormat(teamId, format).getStartingPitcherUserId();
    }

    /**
     * 선발 투수가 지정되어 있는지 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 지정되어 있으면 true
     */
    public boolean hasStartingPitcher(Long teamId, LeagueFormat format) {
        return teamLineupRepository.findByTeamIdAndFormat(teamId, format)
                .map(lineup -> lineup.getStartingPitcherUserId() != null)
                .orElse(false);
    }

    /**
     * 로스터 응답 DTO를 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 로스터 응답
     */
    public TeamLineupResponse getLineupResponse(Long teamId, LeagueFormat format) {
        TeamLineup lineup = getByTeamIdAndFormat(teamId, format);
        return new TeamLineupResponse(
                lineup.getTeamId(),
                lineup.getFormat(),
                List.copyOf(lineup.getUserIds()),
                lineup.getStartingPitcherUserId());
    }
}
