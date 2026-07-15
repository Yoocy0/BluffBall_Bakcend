package com.project.bluffball.domain.game.dto.response;

/**
 * 매치 참가자 접속 상태 — REST state API·재접속 UI용.
 */
public record ParticipantPresenceResponse(
        Long userId,
        boolean online,
        Long disconnectedAtEpochMs,
        Long graceEndsAtEpochMs,
        Integer graceRemainingSeconds
) {
}
