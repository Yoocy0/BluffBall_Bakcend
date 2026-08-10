package com.project.bluffball.domain.store.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 상점 강화 카드 구매 요청.
 *
 * @param enhancementCardId 강화 카드 마스터 ID
 */
public record StoreEnhancementPurchaseRequest(
        @NotNull Long enhancementCardId
) {
}
