package com.project.bluffball.domain.card.dto.response;

/**
 * 좌표 카드 목록 조회 API 응답 DTO.
 */
public record CoordinateCardResponse(
        Long cardId,
        int coordinateNumber,
        String name,
        boolean strike
) {
}
