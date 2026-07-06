package com.project.bluffball.domain.card.dto.response;

import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.game.enums.Timing;

/**
 * 구종 카드 목록 조회 API 응답 DTO.
 */
public record PitchCardResponse(
        Long cardId,
        String name,
        int changeAmount,
        ChangeDirection direction,
        Timing timing
) {
    public static PitchCardResponse from(PitchCard card) {
        return new PitchCardResponse(
                card.getId(),
                card.getName(),
                card.getChangeAmount(),
                card.getDirection(),
                card.getTiming());
    }
}
