package com.project.bluffball.domain.team.dto.response;

import java.util.List;

/**
 * 팀 리그 기록 조회 응답 DTO.
 *
 * <p>{@code aggregate=true}이면 {@code aggregate}에 합산 결과가 채워지고,
 * 목록은 비어 있을 수 있다.</p>
 */
public record TeamRecordsResponse(
        List<TeamRecordItemResponse> records,
        TeamRecordAggregateResponse aggregate
) {
}
