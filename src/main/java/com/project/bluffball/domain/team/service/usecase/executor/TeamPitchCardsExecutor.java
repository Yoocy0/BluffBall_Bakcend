package com.project.bluffball.domain.team.service.usecase.executor;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.team.dto.request.UpsertTeamPitchCardsRequest;
import com.project.bluffball.domain.team.entity.TeamPitchLoadout;
import com.project.bluffball.domain.team.repository.TeamPitchLoadoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 구종 사전 선택 쓰기 전담 Executor (usecase/executor 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamPitchCardsExecutor {

    private final TeamPitchLoadoutRepository teamPitchLoadoutRepository;

    /**
     * 팀·포맷의 멤버별 구종 사전 선택을 통째로 교체한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param selections 멤버별 선택
     * @return 팀 ID
     */
    @Transactional
    public Long replaceAll(
            Long teamId,
            LeagueFormat format,
            List<UpsertTeamPitchCardsRequest.MemberPitchCardSelection> selections) {

        teamPitchLoadoutRepository.deleteByTeamIdAndFormat(teamId, format);
        // delete 후 insert가 같은 트랜잭션에서 충돌하지 않도록 flush
        teamPitchLoadoutRepository.flush();

        for (UpsertTeamPitchCardsRequest.MemberPitchCardSelection selection : selections) {
            teamPitchLoadoutRepository.save(new TeamPitchLoadout(
                    teamId,
                    format,
                    selection.userId(),
                    selection.cardIds(),
                    selection.dropCardId()));
        }
        return teamId;
    }
}
