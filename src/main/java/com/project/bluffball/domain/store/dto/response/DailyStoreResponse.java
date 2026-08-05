package com.project.bluffball.domain.store.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * 일일 상점 응답.
 *
 * @param offerDate   오퍼 날짜 (KST)
 * @param currency    현재 보유 재화
 * @param offers      오늘 노출 구종
 */
public record DailyStoreResponse(
        LocalDate offerDate,
        long currency,
        List<StorePitchOfferResponse> offers
) {
}
