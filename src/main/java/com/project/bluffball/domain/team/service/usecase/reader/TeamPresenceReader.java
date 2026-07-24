package com.project.bluffball.domain.team.service.usecase.reader;

import com.project.bluffball.domain.team.redis.TeamPresence;
import com.project.bluffball.domain.team.repository.TeamPresenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 팀 멤버 프레즌스 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class TeamPresenceReader {

    /** 하트비트 만료 기준 (밀리초). 초과 시 오프라인으로 간주한다. */
    private static final long ONLINE_THRESHOLD_MS = 60_000L;

    private final TeamPresenceRepository teamPresenceRepository;

    /**
     * 멤버 온라인 여부를 반환한다.
     *
     * @param teamId 팀 ID
     * @param userId 유저 ID
     * @return 온라인이면 true
     */
    public boolean isOnline(Long teamId, Long userId) {
        return teamPresenceRepository.findById(String.valueOf(teamId))
                .map(presence -> {
                    TeamPresence.MemberPresence member = presence.findMember(userId);
                    if (member == null || !member.isOnline()) {
                        return false;
                    }
                    long elapsed = System.currentTimeMillis() - member.getLastSeenAtEpochMs();
                    return elapsed <= ONLINE_THRESHOLD_MS;
                })
                .orElse(false);
    }
}
