package com.project.bluffball.gametest.dto;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;

/**
 * 상대 봇 팀을 만들고 매칭 큐에 넣는 요청.
 *
 * @param format 리그 포맷 (null이면 COMPACT)
 * @param tier 큐 티어 (사람 팀과 동일해야 함). null이면 아마4
 */
public record EnqueueOpponentBotRequest(
        LeagueFormat format,
        LeagueTier tier
) {
}
