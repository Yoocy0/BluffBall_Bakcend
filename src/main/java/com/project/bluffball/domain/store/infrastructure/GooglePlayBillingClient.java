package com.project.bluffball.domain.store.infrastructure;

/**
 * Google Play 인앱 구매 검증 클라이언트.
 */
public interface GooglePlayBillingClient {

    /**
     * product 구매를 검증한다.
     *
     * @param productId     SKU
     * @param purchaseToken purchaseToken
     * @return 검증 결과
     */
    GooglePlayPurchaseVerification verifyProductPurchase(String productId, String purchaseToken);

    /**
     * 소비성 상품을 acknowledge 한다 (LIVE에서만 실제 호출).
     *
     * @param productId     SKU
     * @param purchaseToken purchaseToken
     */
    void acknowledgeProductPurchase(String productId, String purchaseToken);
}
