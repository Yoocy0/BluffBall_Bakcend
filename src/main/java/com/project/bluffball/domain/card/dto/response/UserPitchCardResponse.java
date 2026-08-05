package com.project.bluffball.domain.card.dto.response;

import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.game.enums.Timing;

/**
 * 유저 보유 구종 카드(강화 반영) 응답.
 */
public record UserPitchCardResponse(
        Long cardId,
        String name,
        ChangeDirection direction,
        int baseChangeAmount,
        int effectiveChangeAmount,
        boolean changeAmountEnhanced,
        Timing baseTiming,
        Timing effectiveTiming,
        TimingEnhancement timingEnhancement,
        int cost
) {
}
