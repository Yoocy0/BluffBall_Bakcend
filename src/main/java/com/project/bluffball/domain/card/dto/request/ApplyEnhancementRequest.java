package com.project.bluffball.domain.card.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 강화 카드로 구종을 강화하는 요청.
 *
 * @param enhancementCardId 사용할 강화 카드 마스터 ID
 */
public record ApplyEnhancementRequest(
        @NotNull Long enhancementCardId
) {
}
