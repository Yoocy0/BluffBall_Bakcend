package com.project.bluffball.domain.game.redis;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis 저장용 매치 참가자 접속 상태.
 *
 * <p>WebSocket CONNECT·SUBSCRIBE·DISCONNECT 및 heartbeat로 갱신된다.</p>
 */
@RedisHash(value = "MatchPresence", timeToLive = 7200)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchPresence {

    @Id
    private String id;

    private List<ParticipantPresence> participants = new ArrayList<>();

    public MatchPresence(String matchSessionId, List<Long> participantUserIds) {
        this.id = matchSessionId;
        this.participants = new ArrayList<>();
        for (Long userId : participantUserIds) {
            this.participants.add(new ParticipantPresence(userId));
        }
    }

    public ParticipantPresence findParticipant(Long userId) {
        ensureParticipantsInitialized();
        return participants.stream()
                .filter(participant -> userId.equals(participant.getUserId()))
                .findFirst()
                .orElse(null);
    }

    public void ensureParticipantsInitialized() {
        if (participants == null) {
            participants = new ArrayList<>();
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ParticipantPresence {

        private Long userId;
        private boolean online;
        private long lastSeenAtEpochMs;
        private Long disconnectedAtEpochMs;
        private Long graceEndsAtEpochMs;

        ParticipantPresence(Long userId) {
            this.userId = userId;
            this.online = false;
            this.lastSeenAtEpochMs = 0L;
        }

        public void markOnline(long nowEpochMs) {
            this.online = true;
            this.lastSeenAtEpochMs = nowEpochMs;
            this.disconnectedAtEpochMs = null;
            this.graceEndsAtEpochMs = null;
        }

        public void refreshHeartbeat(long nowEpochMs) {
            this.lastSeenAtEpochMs = nowEpochMs;
        }

        public void markOffline(long nowEpochMs, long graceEndsAtEpochMs) {
            this.online = false;
            this.disconnectedAtEpochMs = nowEpochMs;
            this.graceEndsAtEpochMs = graceEndsAtEpochMs;
        }

        void setUserId(Long userId) {
            this.userId = userId;
        }

        void setOnline(boolean online) {
            this.online = online;
        }

        void setLastSeenAtEpochMs(long lastSeenAtEpochMs) {
            this.lastSeenAtEpochMs = lastSeenAtEpochMs;
        }

        void setDisconnectedAtEpochMs(Long disconnectedAtEpochMs) {
            this.disconnectedAtEpochMs = disconnectedAtEpochMs;
        }

        void setGraceEndsAtEpochMs(Long graceEndsAtEpochMs) {
            this.graceEndsAtEpochMs = graceEndsAtEpochMs;
        }
    }
}
