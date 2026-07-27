package com.project.bluffball.gametest.dto;

import com.project.bluffball.domain.game.enums.MatchJoinStatus;
import com.project.bluffball.domain.league.enums.LeagueTier;

import java.util.List;

/**
 * 상대 봇 팀 큐 진입 결과.
 *
 * @param teamId 봇 팀 ID
 * @param leaderUserId 봇 리더
 * @param botUserIds 봇 전원 (리더 포함)
 * @param batterUserIds 타순
 * @param pitcherUserId 투수
 * @param tier 큐 티어
 * @param queueStatus WAITING / MATCHED
 * @param matchSessionId 즉시 매칭 시 세션 ID
 */
public record EnqueueOpponentBotResponse(
        Long teamId,
        Long leaderUserId,
        List<Long> botUserIds,
        List<Long> batterUserIds,
        Long pitcherUserId,
        LeagueTier tier,
        MatchJoinStatus queueStatus,
        String matchSessionId
) {
}
