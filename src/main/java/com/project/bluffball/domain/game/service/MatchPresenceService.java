package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.dto.response.OpponentDisconnectedEvent;
import com.project.bluffball.domain.game.dto.response.OpponentReconnectedEvent;
import com.project.bluffball.domain.game.dto.response.ParticipantPresenceResponse;
import com.project.bluffball.domain.game.redis.MatchPresence;
import com.project.bluffball.domain.game.service.usecase.executor.MatchPresenceExecutor;
import com.project.bluffball.domain.game.service.usecase.reader.GameProgressReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchPresenceReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 매치 참가자 WebSocket 접속 상태 관리 — 120초 유예 후 몰수패.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchPresenceService {

    public static final int GRACE_PERIOD_SECONDS = 120;

    private static final String GAME_TOPIC = "/topic/game/";
    private static final String PRESENCE_TOPIC_SUFFIX = "/presence";

    private final MatchPresenceExecutor matchPresenceExecutor;
    private final MatchPresenceReader matchPresenceReader;
    private final MatchInfoReader matchInfoReader;
    private final GameProgressReader gameProgressReader;
    private final DisconnectForfeitScheduler disconnectForfeitScheduler;
    private final GameForfeitService gameForfeitService;
    private final SimpMessagingTemplate messagingTemplate;

    public void markOnline(String matchSessionId, Long userId) {
        if (gameProgressReader.isGameOver(matchSessionId)) {
            return;
        }

        long nowEpochMs = Instant.now().toEpochMilli();
        MatchPresence presenceBefore = matchPresenceReader.findById(matchSessionId).orElse(null);
        boolean wasOffline = presenceBefore != null
                && presenceBefore.findParticipant(userId) != null
                && !presenceBefore.findParticipant(userId).isOnline();

        disconnectForfeitScheduler.cancel(matchSessionId, userId);
        matchPresenceExecutor.markOnline(matchSessionId, userId, nowEpochMs);

        if (wasOffline) {
            publishReconnectedEvent(matchSessionId, userId);
            log.info("참가자 재접속 matchSessionId={} userId={}", matchSessionId, userId);
        }
    }

    public void markOffline(String matchSessionId, Long userId) {
        if (gameProgressReader.isGameOver(matchSessionId)) {
            return;
        }

        long nowEpochMs = Instant.now().toEpochMilli();
        long graceEndsAtEpochMs = nowEpochMs + (GRACE_PERIOD_SECONDS * 1000L);

        matchPresenceExecutor.markOffline(matchSessionId, userId, nowEpochMs, graceEndsAtEpochMs);
        publishDisconnectedEvent(matchSessionId, userId, nowEpochMs, graceEndsAtEpochMs);
        scheduleForfeit(matchSessionId, userId, graceEndsAtEpochMs);

        log.info("참가자 접속 끊김 matchSessionId={} userId={} graceEndsAt={}",
                matchSessionId, userId, Instant.ofEpochMilli(graceEndsAtEpochMs));
    }

    public void refreshHeartbeat(String matchSessionId, Long userId) {
        if (gameProgressReader.isGameOver(matchSessionId)) {
            return;
        }

        matchPresenceExecutor.refreshHeartbeat(matchSessionId, userId, Instant.now().toEpochMilli());
    }

    public List<ParticipantPresenceResponse> getParticipantPresenceResponses(String matchSessionId) {
        List<Long> participantUserIds = matchInfoReader.getParticipantUserIds(matchSessionId);
        MatchPresence presence = matchPresenceReader.findById(matchSessionId).orElse(null);
        long nowEpochMs = Instant.now().toEpochMilli();

        List<ParticipantPresenceResponse> responses = new ArrayList<>();
        for (Long userId : participantUserIds) {
            if (presence == null) {
                responses.add(new ParticipantPresenceResponse(userId, false, null, null, null));
                continue;
            }

            MatchPresence.ParticipantPresence participant = presence.findParticipant(userId);
            if (participant == null) {
                responses.add(new ParticipantPresenceResponse(userId, false, null, null, null));
                continue;
            }

            responses.add(toResponse(participant, nowEpochMs));
        }
        return responses;
    }

    private void scheduleForfeit(String matchSessionId, Long userId, long graceEndsAtEpochMs) {
        disconnectForfeitScheduler.schedule(
                matchSessionId,
                userId,
                Instant.ofEpochMilli(graceEndsAtEpochMs),
                () -> executeForfeitIfStillOffline(matchSessionId, userId));
    }

    private void executeForfeitIfStillOffline(String matchSessionId, Long userId) {
        if (gameProgressReader.isGameOver(matchSessionId)) {
            return;
        }

        MatchPresence presence = matchPresenceReader.findById(matchSessionId).orElse(null);
        if (presence == null) {
            return;
        }

        MatchPresence.ParticipantPresence participant = presence.findParticipant(userId);
        if (participant == null || participant.isOnline()) {
            return;
        }

        Long graceEndsAtEpochMs = participant.getGraceEndsAtEpochMs();
        if (graceEndsAtEpochMs == null || Instant.now().toEpochMilli() < graceEndsAtEpochMs) {
            return;
        }

        gameForfeitService.applyDisconnectForfeit(matchSessionId, userId);
    }

    private ParticipantPresenceResponse toResponse(MatchPresence.ParticipantPresence participant,
                                                   long nowEpochMs) {
        Integer graceRemainingSeconds = null;
        if (!participant.isOnline() && participant.getGraceEndsAtEpochMs() != null) {
            long remainingMs = participant.getGraceEndsAtEpochMs() - nowEpochMs;
            graceRemainingSeconds = remainingMs > 0 ? (int) Math.ceil(remainingMs / 1000.0) : 0;
        }

        return new ParticipantPresenceResponse(
                participant.getUserId(),
                participant.isOnline(),
                participant.getDisconnectedAtEpochMs(),
                participant.getGraceEndsAtEpochMs(),
                graceRemainingSeconds);
    }

    private void publishDisconnectedEvent(String matchSessionId,
                                          Long disconnectedUserId,
                                          long disconnectedAtEpochMs,
                                          long graceEndsAtEpochMs) {
        OpponentDisconnectedEvent event = new OpponentDisconnectedEvent(
                disconnectedUserId,
                disconnectedAtEpochMs,
                graceEndsAtEpochMs,
                GRACE_PERIOD_SECONDS);
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId + PRESENCE_TOPIC_SUFFIX, event);
    }

    private void publishReconnectedEvent(String matchSessionId, Long reconnectedUserId) {
        OpponentReconnectedEvent event = new OpponentReconnectedEvent(reconnectedUserId);
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId + PRESENCE_TOPIC_SUFFIX, event);
    }
}
