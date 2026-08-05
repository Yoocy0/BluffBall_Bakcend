package com.project.bluffball.domain.store.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Google Play 인앱 결제 확인 요청.
 *
 * @param productId     Play Console productId (SKU)
 * @param purchaseToken 클라이언트 결제 후 받은 purchaseToken
 */
public record GooglePlayConfirmRequest(
        @NotBlank String productId,
        @NotBlank String purchaseToken
) {
}
