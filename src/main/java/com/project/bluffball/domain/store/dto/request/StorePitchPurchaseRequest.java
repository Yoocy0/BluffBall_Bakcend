package com.project.bluffball.domain.store.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 일일 상점 구종 구매 요청.
 *
 * @param cardId 오늘 오퍼에 포함된 마스터 구종 ID
 */
public record StorePitchPurchaseRequest(
        @NotNull Long cardId
) {
}
