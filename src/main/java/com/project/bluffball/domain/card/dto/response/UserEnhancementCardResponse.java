package com.project.bluffball.domain.card.dto.response;

import com.project.bluffball.domain.card.enums.EnhancementEffect;

/**
 * 유저 보유 강화 카드 응답.
 *
 * @param cardId   마스터 강화 카드 ID
 * @param name     이름
 * @param effect   효과
 * @param quantity 보유 수량
 */
public record UserEnhancementCardResponse(
        Long cardId,
        String name,
        EnhancementEffect effect,
        int quantity
) {
}
