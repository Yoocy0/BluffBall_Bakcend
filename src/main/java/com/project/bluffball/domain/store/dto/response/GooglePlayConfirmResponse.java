package com.project.bluffball.domain.store.dto.response;

/**
 * Google Play 결제 확인·재화 지급 결과.
 *
 * @param productId         상품 ID
 * @param currencyGranted   지급 재화
 * @param remainingCurrency 지급 후 잔액
 */
public record GooglePlayConfirmResponse(
        String productId,
        long currencyGranted,
        long remainingCurrency
) {
}
