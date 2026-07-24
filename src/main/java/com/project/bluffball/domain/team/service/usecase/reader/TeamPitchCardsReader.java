package com.project.bluffball.domain.team.service.usecase.reader;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.team.dto.response.TeamPitchCardsResponse;
import com.project.bluffball.domain.team.entity.TeamPitchLoadout;
import com.project.bluffball.domain.team.repository.TeamPitchLoadoutRepository;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 구종 사전 선택 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamPitchCardsReader {

    private final TeamPitchLoadoutRepository teamPitchLoadoutRepository;

    /**
     * 팀·포맷의 사전 선택 개수를 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 개수
     */
    public long count(Long teamId, LeagueFormat format) {
        return teamPitchLoadoutRepository.countByTeamIdAndFormat(teamId, format);
    }

    /**
     * 로스터 전원에 대해 사전 선택이 완료됐는지 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param rosterSize 로스터 인원
     * @return 완료면 true
     */
    public boolean isComplete(Long teamId, LeagueFormat format, int rosterSize) {
        return count(teamId, format) == rosterSize;
    }

    /**
     * 사전 선택 응답 DTO를 반환한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 사전 선택 응답
     * @throws NotFoundException 선택이 하나도 없으면
     */
    public TeamPitchCardsResponse getPitchCardsResponse(Long teamId, LeagueFormat format) {
        List<TeamPitchLoadout> loadouts =
                teamPitchLoadoutRepository.findByTeamIdAndFormat(teamId, format);
        if (loadouts.isEmpty()) {
            throw new NotFoundException(
                    ErrorCode.TEAM_PITCH_CARDS_NOT_FOUND,
                    "teamId=" + teamId + ", format=" + format);
        }
        List<TeamPitchCardsResponse.MemberPitchCards> selections = loadouts.stream()
                .map(loadout -> new TeamPitchCardsResponse.MemberPitchCards(
                        loadout.getUserId(),
                        List.copyOf(loadout.getCardIds()),
                        loadout.getDropCardId()))
                .toList();
        return new TeamPitchCardsResponse(teamId, format, selections);
    }
}
