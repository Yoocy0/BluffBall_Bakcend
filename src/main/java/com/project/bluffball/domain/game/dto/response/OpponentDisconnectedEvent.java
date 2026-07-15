package com.project.bluffball.domain.game.dto.response;

/**
 * 접속 끊김 WebSocket 송신 이벤트.
 */
public record OpponentDisconnectedEvent(
        Long disconnectedUserId,
        long disconnectedAtEpochMs,
        long graceEndsAtEpochMs,
        int gracePeriodSeconds
) {
}
