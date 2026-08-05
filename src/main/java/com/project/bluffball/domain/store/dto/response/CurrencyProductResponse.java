package com.project.bluffball.domain.store.dto.response;

/**
 * 재화 인앱 상품.
 *
 * @param productId      Play SKU
 * @param currencyAmount 지급 재화
 */
public record CurrencyProductResponse(
        String productId,
        long currencyAmount
) {
}
