package com.project.bluffball.gametest.dto;

import com.project.bluffball.gametest.bot.BotRole;

import java.util.List;

/**
 * Compact 로스터 빈자리 채움 결과 (테스트용).
 *
 * @param teamId 팀 ID
 * @param leaderUserId 리더
 * @param batterUserIds 타순 3명
 * @param pitcherUserId 전담 투수
 * @param createdBots 이번에 생성한 봇
 * @param keptExistingLineup 기존 라인업 유지 여부
 * @param leagueEntered 이번 호출에서 리그 진입했는지
 */
public record FillCompactRosterResponse(
        Long teamId,
        Long leaderUserId,
        List<Long> batterUserIds,
        Long pitcherUserId,
        List<CreatedBot> createdBots,
        boolean keptExistingLineup,
        boolean leagueEntered
) {

    /**
     * 생성된 봇 한 명.
     *
     * @param userId 유저 ID
     * @param role 역할
     */
    public record CreatedBot(Long userId, BotRole role) {
    }
}
