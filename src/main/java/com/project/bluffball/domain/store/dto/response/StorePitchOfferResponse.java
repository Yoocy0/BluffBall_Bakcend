package com.project.bluffball.domain.store.dto.response;

/**
 * 상점 구종(기본본) 판매 항목.
 *
 * @param cardId      마스터 구종 ID
 * @param name        이름
 * @param price       가격
 * @param hasBaseCopy 미강화 기본본 보유 여부
 * @param purchasable 기본본 구매 가능 (기본본 미보유 시)
 */
public record StorePitchOfferResponse(
        Long cardId,
        String name,
        long price,
        boolean hasBaseCopy,
        boolean purchasable
) {
}
