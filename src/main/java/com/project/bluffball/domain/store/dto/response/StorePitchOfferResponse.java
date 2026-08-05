package com.project.bluffball.domain.store.dto.response;

/**
 * 일일 상점 구종 오퍼 한 칸.
 *
 * @param cardId      마스터 구종 ID
 * @param name        구종 이름
 * @param price       가격(재화)
 * @param owned       이미 보유 여부
 * @param purchasedToday 오늘 이미 구매했는지
 * @param purchasable 지금 구매 가능 여부
 */
public record StorePitchOfferResponse(
        Long cardId,
        String name,
        long price,
        boolean owned,
        boolean purchasedToday,
        boolean purchasable
) {
}
