package com.project.bluffball.domain.card.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 유저 보유 강화 카드 (수량).
 *
 * <p>식별: {@code (userId, enhancementCardId)}. 강화 적용 시 1장 소모한다.</p>
 */
@Entity
@Table(
        name = "user_enhancement_card",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_enhancement_card_user_card",
                columnNames = {"user_id", "enhancement_card_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEnhancementCard {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_enhancement_card_id")
    private Long id;

    /** 유저 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 마스터 강화 카드 ID */
    @Column(name = "enhancement_card_id", nullable = false)
    private Long enhancementCardId;

    /** 보유 수량 */
    @Column(name = "quantity", nullable = false)
    private int quantity;

    /** 최초 획득 시각 */
    @Column(name = "acquired_at", nullable = false, updatable = false)
    private LocalDateTime acquiredAt;

    /** 마지막 변경 시각 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 보유 행을 생성한다.
     *
     * @param userId             유저 ID
     * @param enhancementCardId  마스터 강화 카드 ID
     * @param quantity           초기 수량 (1 이상)
     */
    public UserEnhancementCard(Long userId, Long enhancementCardId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("초기 수량은 1 이상이어야 합니다.");
        }
        this.userId = userId;
        this.enhancementCardId = enhancementCardId;
        this.quantity = quantity;
    }

    /**
     * 수량을 추가한다.
     *
     * @param amount 추가량 (1 이상)
     */
    public void addQuantity(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("추가 수량은 1 이상이어야 합니다.");
        }
        this.quantity += amount;
    }

    /**
     * 수량을 1장 소모한다.
     *
     * @throws IllegalStateException 수량이 부족할 때
     */
    public void consumeOne() {
        if (this.quantity < 1) {
            throw new IllegalStateException("강화 카드 수량이 부족합니다.");
        }
        this.quantity -= 1;
    }

    /**
     * 보유 중인지.
     *
     * @return quantity &gt; 0
     */
    public boolean hasStock() {
        return this.quantity > 0;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now(KST);
        this.acquiredAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now(KST);
    }
}
