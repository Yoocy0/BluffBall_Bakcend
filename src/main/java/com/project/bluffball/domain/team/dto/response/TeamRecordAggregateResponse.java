package com.project.bluffball.domain.team.dto.response;

/**
 * 팀 리그 기록 종합(합산) 응답 DTO.
 */
public record TeamRecordAggregateResponse(
        int wins,
        int losses,
        int runDiff,
        int seasonCount
) {
}
