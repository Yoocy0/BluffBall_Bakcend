package com.project.bluffball.domain.team.service.usecase.executor;

import com.project.bluffball.domain.team.redis.TeamPresence;
import com.project.bluffball.domain.team.repository.TeamPresenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 팀 멤버 프레즌스 쓰기 전담 Executor (usecase/executor 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamPresenceExecutor {

    private final TeamPresenceRepository teamPresenceRepository;

    /**
     * 멤버 하트비트를 갱신한다.
     *
     * @param teamId 팀 ID
     * @param userId 유저 ID
     */
    public void heartbeat(Long teamId, Long userId) {
        String presenceId = String.valueOf(teamId);
        TeamPresence presence = teamPresenceRepository.findById(presenceId)
                .orElseGet(() -> new TeamPresence(teamId));
        presence.ensureMembersInitialized();
        TeamPresence.MemberPresence member = presence.getOrCreateMember(userId);
        member.refreshHeartbeat(System.currentTimeMillis());
        teamPresenceRepository.save(presence);
    }
}
