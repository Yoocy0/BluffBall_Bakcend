package com.project.bluffball.domain.league.dto.response;

/**
 * 순위별 상금 항목 응답 DTO.
 */
public record LeaguePrizeItemResponse(
        int rank,
        /** 1등 상금 대비 비율 (1등은 100) */
        int percentOfFirst,
        long prizeAmount
) {
}
