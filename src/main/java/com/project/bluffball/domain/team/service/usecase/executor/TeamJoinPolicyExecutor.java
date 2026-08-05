package com.project.bluffball.domain.team.service.usecase.executor;

import com.project.bluffball.domain.team.entity.Team;
import com.project.bluffball.domain.team.enums.TeamJoinPolicy;
import com.project.bluffball.domain.team.repository.TeamRepository;
import com.project.bluffball.domain.team.service.usecase.reader.TeamReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 가입 정책 변경 쓰기 전담 Executor.
 */
@Component
@RequiredArgsConstructor
public class TeamJoinPolicyExecutor {

    /** 팀 Repository */
    private final TeamRepository teamRepository;

    /** 팀 Reader */
    private final TeamReader teamReader;

    /**
     * 가입 정책을 변경한다.
     *
     * @param teamId 팀 ID
     * @param joinPolicy 새 정책
     * @return 팀 ID
     */
    @Transactional
    public Long updateJoinPolicy(Long teamId, TeamJoinPolicy joinPolicy) {
        Team team = teamReader.getById(teamId);
        team.updateJoinPolicy(joinPolicy);
        teamRepository.save(team);
        return teamId;
    }
}
