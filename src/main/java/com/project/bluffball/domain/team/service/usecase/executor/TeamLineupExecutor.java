package com.project.bluffball.domain.team.service.usecase.executor;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.team.entity.TeamLineup;
import com.project.bluffball.domain.team.repository.TeamLineupRepository;
import com.project.bluffball.domain.team.repository.TeamPitchLoadoutRepository;
import com.project.bluffball.domain.team.service.usecase.reader.TeamLineupReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 출전 로스터 쓰기 전담 Executor (usecase/executor 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamLineupExecutor {

    private final TeamLineupRepository teamLineupRepository;
    private final TeamPitchLoadoutRepository teamPitchLoadoutRepository;
    private final TeamLineupReader teamLineupReader;

    /**
     * 출전 로스터(타순·선발 투수)를 생성하거나 갱신한다.
     *
     * <p>로스터에서 빠진 멤버의 구종 사전 선택은 삭제한다.</p>
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param userIds 타순
     * @param startingPitcherUserId 선발 투수
     * @return 팀 ID
     */
    @Transactional
    public Long upsert(
            Long teamId,
            LeagueFormat format,
            List<Long> userIds,
            Long startingPitcherUserId) {
        List<Long> nextUserIds = List.copyOf(userIds);

        teamLineupRepository.findByTeamIdAndFormat(teamId, format)
                .ifPresentOrElse(
                        lineup -> {
                            List<Long> previous = new ArrayList<>(lineup.getUserIds());
                            lineup.replaceLineup(nextUserIds, startingPitcherUserId);
                            teamLineupRepository.save(lineup);
                            deleteOrphanPitchLoadouts(teamId, format, previous, nextUserIds);
                        },
                        () -> teamLineupRepository.save(
                                new TeamLineup(teamId, format, nextUserIds, startingPitcherUserId))
                );

        teamLineupReader.getByTeamIdAndFormat(teamId, format);
        return teamId;
    }

    /**
     * 로스터에서 제외된 멤버의 사전 선택을 삭제한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param previous 이전 로스터
     * @param next 새 로스터
     */
    private void deleteOrphanPitchLoadouts(
            Long teamId,
            LeagueFormat format,
            List<Long> previous,
            List<Long> next) {
        Set<Long> nextSet = new HashSet<>(next);
        List<Long> removed = previous.stream()
                .filter(userId -> !nextSet.contains(userId))
                .toList();
        if (!removed.isEmpty()) {
            teamPitchLoadoutRepository.deleteByTeamIdAndFormatAndUserIdIn(teamId, format, removed);
        }
    }
}
