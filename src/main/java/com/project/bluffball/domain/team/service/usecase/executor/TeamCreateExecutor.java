package com.project.bluffball.domain.team.service.usecase.executor;

import com.project.bluffball.domain.team.entity.Team;
import com.project.bluffball.domain.team.entity.TeamMember;
import com.project.bluffball.domain.team.enums.TeamJoinPolicy;
import com.project.bluffball.domain.team.enums.TeamMemberRole;
import com.project.bluffball.domain.team.repository.TeamMemberRepository;
import com.project.bluffball.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 창단 쓰기 전담 Executor (usecase/executor 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamCreateExecutor {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * 팀과 리더 멤버십을 원자적으로 생성한다.
     *
     * @param name 팀 이름
     * @param leaderUserId 창단자(리더) 유저 ID
     * @param logoUrl 로고 URL (nullable)
     * @param joinPolicy 가입 정책 (nullable → OPEN)
     * @return 생성된 팀 ID
     */
    @Transactional
    public Long create(String name, Long leaderUserId, String logoUrl, TeamJoinPolicy joinPolicy) {
        Team team = teamRepository.save(new Team(name, leaderUserId, logoUrl, joinPolicy));
        teamMemberRepository.save(new TeamMember(team.getId(), leaderUserId, TeamMemberRole.LEADER));
        return team.getId();
    }
}
