package com.project.bluffball.domain.store.dto.response;

import com.project.bluffball.domain.card.enums.EnhancementEffect;

/**
 * 상점 강화 카드 판매 칸.
 *
 * @param cardId   강화 카드 마스터 ID
 * @param name     이름
 * @param effect   효과
 * @param price    가격(재화)
 * @param ownedQty 현재 보유 수량
 */
public record StoreEnhancementOfferResponse(
        Long cardId,
        String name,
        EnhancementEffect effect,
        long price,
        int ownedQty
) {
}
