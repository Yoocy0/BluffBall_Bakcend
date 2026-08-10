package com.project.bluffball.domain.store.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 상점 구종 기본본 구매 요청.
 *
 * @param cardId 마스터 구종 ID
 */
public record StorePitchPurchaseRequest(
        @NotNull Long cardId
) {
}
