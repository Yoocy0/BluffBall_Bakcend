package com.project.bluffball.domain.store.dto.response;

import com.project.bluffball.domain.card.dto.response.UserPitchCardResponse;

/**
 * 구종 구매 결과.
 *
 * @param remainingCurrency 차감 후 잔액
 * @param purchasedCard     구매한 구종
 */
public record StorePitchPurchaseResponse(
        long remainingCurrency,
        UserPitchCardResponse purchasedCard
) {
}
