package com.project.bluffball.domain.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Google Play 인앱 결제 처리 기록 (purchaseToken 멱등).
 */
@Entity
@Table(
        name = "google_play_purchase",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_google_play_purchase_token", columnNames = {"purchase_token"}),
                @UniqueConstraint(name = "uk_google_play_order_id", columnNames = {"order_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GooglePlayPurchase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "google_play_purchase_id")
    private Long id;

    /** 유저 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Play productId (SKU) */
    @Column(name = "product_id", nullable = false, length = 128)
    private String productId;

    /** Play purchaseToken */
    @Column(name = "purchase_token", nullable = false, length = 512)
    private String purchaseToken;

    /** Play orderId (없을 수 있어 nullable unique는 null 허용) */
    @Column(name = "order_id", length = 128)
    private String orderId;

    /** 지급한 재화량 */
    @Column(name = "currency_amount", nullable = false)
    private long currencyAmount;

    /** 처리 시각 */
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;

    /**
     * 결제 처리 기록을 생성한다.
     *
     * @param userId         유저 ID
     * @param productId      상품 ID
     * @param purchaseToken  구매 토큰
     * @param orderId        주문 ID (없으면 null)
     * @param currencyAmount 지급 재화
     */
    public GooglePlayPurchase(
            Long userId,
            String productId,
            String purchaseToken,
            String orderId,
            long currencyAmount) {
        this.userId = userId;
        this.productId = productId;
        this.purchaseToken = purchaseToken;
        this.orderId = orderId;
        this.currencyAmount = currencyAmount;
    }

    @PrePersist
    private void prePersist() {
        this.processedAt = LocalDateTime.now(KST);
    }
}
