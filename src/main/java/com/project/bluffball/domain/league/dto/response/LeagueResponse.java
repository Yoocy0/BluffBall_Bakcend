package com.project.bluffball.domain.league.dto.response;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;

/**
 * 리그 카탈로그(등급) 응답 DTO.
 */
public record LeagueResponse(
        Long leagueId,
        LeagueFormat format,
        LeagueTier tier,
        String name,
        long entryFee,
        long firstPlacePrize
) {
}
