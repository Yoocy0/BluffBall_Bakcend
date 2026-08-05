package com.project.bluffball.domain.store.infrastructure;

/**
 * Google Play 인앱 결제 검증 결과.
 *
 * @param valid         유효한 구매인지
 * @param orderId       주문 ID (없으면 null)
 * @param purchaseState 0=purchased, 1=canceled, 2=pending
 * @param message       실패/상태 메시지
 */
public record GooglePlayPurchaseVerification(
        boolean valid,
        String orderId,
        Integer purchaseState,
        String message
) {
    /**
     * 유효 구매 결과.
     *
     * @param orderId 주문 ID
     * @return 검증 결과
     */
    public static GooglePlayPurchaseVerification valid(String orderId) {
        return new GooglePlayPurchaseVerification(true, orderId, 0, null);
    }

    /**
     * 무효 구매 결과.
     *
     * @param message 사유
     * @return 검증 결과
     */
    public static GooglePlayPurchaseVerification invalid(String message) {
        return new GooglePlayPurchaseVerification(false, null, null, message);
    }
}
