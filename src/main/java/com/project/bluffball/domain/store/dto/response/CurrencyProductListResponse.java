package com.project.bluffball.domain.store.dto.response;

import java.util.List;

/**
 * 재화 상품 목록 응답.
 *
 * @param products 상품 목록
 */
public record CurrencyProductListResponse(
        List<CurrencyProductResponse> products
) {
}
