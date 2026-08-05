package com.project.bluffball.domain.store.infrastructure;

import com.project.bluffball.global.config.BillingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 개발용 Google Play 검증 (Play API 미호출).
 *
 * <p>{@code billing.google.verification-mode=MOCK} 일 때 활성화.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "billing.google", name = "verification-mode", havingValue = "MOCK", matchIfMissing = true)
public class MockGooglePlayBillingClient implements GooglePlayBillingClient {

    @Override
    public GooglePlayPurchaseVerification verifyProductPurchase(String productId, String purchaseToken) {
        log.warn("MOCK Google Play verification used. productId={}", productId);
        if (purchaseToken == null || purchaseToken.isBlank()) {
            return GooglePlayPurchaseVerification.invalid("purchaseToken empty");
        }
        return GooglePlayPurchaseVerification.valid("mock-" + Integer.toHexString(purchaseToken.hashCode())
                + "-" + purchaseToken.length());
    }

    @Override
    public void acknowledgeProductPurchase(String productId, String purchaseToken) {
        log.debug("MOCK acknowledge skipped. productId={}", productId);
    }
}
