package com.project.bluffball.domain.store.repository;

import com.project.bluffball.domain.store.entity.GooglePlayPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Google Play 결제 처리 Repository.
 */
public interface GooglePlayPurchaseRepository extends JpaRepository<GooglePlayPurchase, Long> {

    /**
     * purchaseToken 처리 여부.
     *
     * @param purchaseToken 구매 토큰
     * @return 이미 처리됐으면 true
     */
    boolean existsByPurchaseToken(String purchaseToken);

    /**
     * orderId 처리 여부.
     *
     * @param orderId 주문 ID
     * @return 이미 처리됐으면 true
     */
    boolean existsByOrderId(String orderId);
}
