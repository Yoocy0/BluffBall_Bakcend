package com.project.bluffball.domain.card.dto.response;

import com.project.bluffball.domain.card.entity.EnhancementCard;
import com.project.bluffball.domain.card.enums.EnhancementEffect;

/**
 * 강화 카드 마스터 응답.
 *
 * @param cardId 마스터 카드 ID
 * @param name   이름
 * @param effect 효과
 */
public record EnhancementCardResponse(
        Long cardId,
        String name,
        EnhancementEffect effect
) {
    /**
     * 엔티티 → 응답.
     *
     * @param card 마스터
     * @return 응답
     */
    public static EnhancementCardResponse from(EnhancementCard card) {
        return new EnhancementCardResponse(card.getId(), card.getName(), card.getEffect());
    }
}
