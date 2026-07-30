package com.project.bluffball.gametest.dto;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.gametest.bot.BotRole;
import jakarta.validation.constraints.NotNull;

/**
 * 리그 로스터 빈자리 채움 요청 (테스트용).
 *
 * <p>부족한 인원 수는 클라이언트가 넘기지 않는다.
 * 서버가 {@code format} 기준(Compact 4 / Full 9)으로 빈자리를 계산한다.</p>
 *
 * @param leaderUserId 팀 리더(사람) userId
 * @param teamId 팀 ID (없으면 리더 소속 팀 사용, 없으면 창단)
 * @param format COMPACT 또는 FULL
 * @param myRole 라인업을 새로 짤 때 리더 역할 (기본 BATTER)
 */
public record FillRosterRequest(
        @NotNull Long leaderUserId,
        Long teamId,
        @NotNull LeagueFormat format,
        BotRole myRole
) {
}
