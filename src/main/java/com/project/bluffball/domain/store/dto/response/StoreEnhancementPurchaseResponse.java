package com.project.bluffball.domain.store.dto.response;

import com.project.bluffball.domain.card.dto.response.UserEnhancementCardResponse;

/**
 * 강화 카드 구매 결과.
 *
 * @param remainingCurrency 차감 후 잔액
 * @param purchasedCard     구매 후 해당 강화 카드 보유
 */
public record StoreEnhancementPurchaseResponse(
        long remainingCurrency,
        UserEnhancementCardResponse purchasedCard
) {
}
