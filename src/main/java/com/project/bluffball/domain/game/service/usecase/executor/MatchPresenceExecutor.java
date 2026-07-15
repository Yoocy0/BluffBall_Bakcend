package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.redis.MatchPresence;
import com.project.bluffball.domain.game.repository.MatchPresenceRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchPresenceReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * MatchPresence Redis 쓰기 전담 Executor.
 */
@Component
@RequiredArgsConstructor
public class MatchPresenceExecutor {

    private final MatchPresenceRepository matchPresenceRepository;
    private final MatchPresenceReader matchPresenceReader;
    private final MatchInfoReader matchInfoReader;

    public MatchPresence getOrCreate(String matchSessionId) {
        return matchPresenceRepository.findById(matchSessionId)
                .orElseGet(() -> {
                    MatchPresence created = new MatchPresence(
                            matchSessionId,
                            matchInfoReader.getParticipantUserIds(matchSessionId));
                    return matchPresenceRepository.save(created);
                });
    }

    public void save(MatchPresence matchPresence) {
        matchPresenceRepository.save(matchPresence);
    }

    public void markOnline(String matchSessionId, Long userId, long nowEpochMs) {
        MatchPresence presence = getOrCreate(matchSessionId);
        MatchPresence.ParticipantPresence participant = requireParticipant(presence, userId);
        participant.markOnline(nowEpochMs);
        matchPresenceRepository.save(presence);
    }

    public void refreshHeartbeat(String matchSessionId, Long userId, long nowEpochMs) {
        MatchPresence presence = matchPresenceReader.findById(matchSessionId).orElse(null);
        if (presence == null) {
            return;
        }

        MatchPresence.ParticipantPresence participant = presence.findParticipant(userId);
        if (participant == null || !participant.isOnline()) {
            return;
        }

        participant.refreshHeartbeat(nowEpochMs);
        matchPresenceRepository.save(presence);
    }

    public void markOffline(String matchSessionId, Long userId, long nowEpochMs, long graceEndsAtEpochMs) {
        MatchPresence presence = getOrCreate(matchSessionId);
        MatchPresence.ParticipantPresence participant = requireParticipant(presence, userId);
        if (!participant.isOnline()) {
            return;
        }

        participant.markOffline(nowEpochMs, graceEndsAtEpochMs);
        matchPresenceRepository.save(presence);
    }

    private MatchPresence.ParticipantPresence requireParticipant(MatchPresence presence, Long userId) {
        MatchPresence.ParticipantPresence participant = presence.findParticipant(userId);
        if (participant == null) {
            throw new IllegalStateException(
                    "MatchPresence participant not found: matchSessionId=" + presence.getId() + ", userId=" + userId);
        }
        return participant;
    }
}
