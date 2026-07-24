package com.project.bluffball.domain.league.dto.response;

import java.util.List;

/**
 * 리그 시즌 순위별 상금표 응답 DTO.
 *
 * <p>2등 이후 상금은 1등 상금의 퍼센트로 산정한다.</p>
 */
public record LeaguePrizeResponse(
        Long seasonId,
        Long leagueId,
        long firstPlacePrize,
        List<LeaguePrizeItemResponse> prizes
) {
}
