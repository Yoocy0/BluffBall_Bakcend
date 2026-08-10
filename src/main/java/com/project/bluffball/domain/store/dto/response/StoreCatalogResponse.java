package com.project.bluffball.domain.store.dto.response;

import java.util.List;

/**
 * 상시 상점 카탈로그.
 *
 * @param currency          보유 재화
 * @param pitchOffers       구종 기본본 목록
 * @param enhancementOffers 강화 카드 목록
 */
public record StoreCatalogResponse(
        long currency,
        List<StorePitchOfferResponse> pitchOffers,
        List<StoreEnhancementOfferResponse> enhancementOffers
) {
}
