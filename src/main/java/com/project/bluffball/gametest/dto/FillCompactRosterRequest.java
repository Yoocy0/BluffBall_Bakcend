package com.project.bluffball.gametest.dto;

import com.project.bluffball.gametest.bot.BotRole;
import jakarta.validation.constraints.NotNull;

/**
 * Compact 로스터 빈자리 채움 요청 (테스트용).
 *
 * @param leaderUserId 팀 리더(사람) userId
 * @param teamId 팀 ID (없으면 리더 소속 팀 사용, 없으면 창단)
 * @param myRole 라인업을 새로 짤 때 리더 역할 (기본 BATTER)
 */
public record FillCompactRosterRequest(
        @NotNull Long leaderUserId,
        Long teamId,
        BotRole myRole
) {
}
