package com.project.bluffball.domain.team.redis;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis 저장용 팀 멤버 접속 상태.
 *
 * <p>팀 멤버 목록의 온/오프라인 표시와 heartbeat API로 갱신된다.</p>
 */
@RedisHash(value = "TeamPresence", timeToLive = 86400)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamPresence {

    /** 팀 ID 문자열 */
    @Id
    private String id;

    private List<MemberPresence> members = new ArrayList<>();

    public TeamPresence(Long teamId) {
        this.id = String.valueOf(teamId);
        this.members = new ArrayList<>();
    }

    public Long getTeamId() {
        return Long.valueOf(id);
    }

    public MemberPresence findMember(Long userId) {
        ensureMembersInitialized();
        return members.stream()
                .filter(member -> userId.equals(member.getUserId()))
                .findFirst()
                .orElse(null);
    }

    public MemberPresence getOrCreateMember(Long userId) {
        MemberPresence existing = findMember(userId);
        if (existing != null) {
            return existing;
        }
        MemberPresence created = new MemberPresence(userId);
        members.add(created);
        return created;
    }

    public void ensureMembersInitialized() {
        if (members == null) {
            members = new ArrayList<>();
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class MemberPresence {

        private Long userId;
        private boolean online;
        private long lastSeenAtEpochMs;

        MemberPresence(Long userId) {
            this.userId = userId;
            this.online = false;
            this.lastSeenAtEpochMs = 0L;
        }

        public void markOnline(long nowEpochMs) {
            this.online = true;
            this.lastSeenAtEpochMs = nowEpochMs;
        }

        public void refreshHeartbeat(long nowEpochMs) {
            this.online = true;
            this.lastSeenAtEpochMs = nowEpochMs;
        }

        public void markOffline() {
            this.online = false;
        }
    }
}
