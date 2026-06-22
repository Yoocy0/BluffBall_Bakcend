package com.project.bluffball.domain.game.dto.response;

import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.game.enums.Timing;

/**
 * 투수 카드 패 내 개별 카드 정보 DTO.
 */
public record CardInfo(
        Long cardId,
        String name,
        int changeAmount,
        ChangeDirection direction,
        Timing timing
) {
}
