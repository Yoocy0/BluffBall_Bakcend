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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 일일 상점 구종 구매 기록 (당일 동일 구종 중복 구매 방지).
 */
@Entity
@Table(
        name = "store_pitch_purchase",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_store_pitch_purchase_user_date_card",
                columnNames = {"user_id", "offer_date", "card_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorePitchPurchase {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_pitch_purchase_id")
    private Long id;

    /** 구매 유저 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 상점 오퍼 날짜 (KST) */
    @Column(name = "offer_date", nullable = false)
    private LocalDate offerDate;

    /** 구매한 마스터 구종 ID */
    @Column(name = "card_id", nullable = false)
    private Long cardId;

    /** 지불 재화 */
    @Column(name = "price_paid", nullable = false)
    private long pricePaid;

    /** 구매 시각 */
    @Column(name = "purchased_at", nullable = false, updatable = false)
    private LocalDateTime purchasedAt;

    /**
     * 구종 구매 기록을 생성한다.
     *
     * @param userId    유저 ID
     * @param offerDate 오퍼 날짜
     * @param cardId    구종 ID
     * @param pricePaid 지불액
     */
    public StorePitchPurchase(Long userId, LocalDate offerDate, Long cardId, long pricePaid) {
        this.userId = userId;
        this.offerDate = offerDate;
        this.cardId = cardId;
        this.pricePaid = pricePaid;
    }

    @PrePersist
    private void prePersist() {
        this.purchasedAt = LocalDateTime.now(KST);
    }
}
