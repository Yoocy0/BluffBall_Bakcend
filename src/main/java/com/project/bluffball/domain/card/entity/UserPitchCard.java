package com.project.bluffball.domain.card.entity;

import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.game.enums.Timing;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 유저 보유 구종 카드 인스턴스 (마스터 {@link PitchCard} + 강화 오버레이).
 *
 * <p>동일 마스터 구종을 여러 장(기본본·강화본) 보유할 수 있다.
 * 식별은 {@code userPitchCardId}이며, 로드아웃·강화 API는 이 ID를 사용한다.</p>
 */
@Entity
@Table(
        name = "user_pitch_card",
        indexes = @Index(name = "ix_user_pitch_card_user_master", columnList = "user_id, card_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPitchCard {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_pitch_card_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 마스터 구종 카드 ID ({@code pitch_card.card_id}) */
    @Column(name = "card_id", nullable = false)
    private Long cardId;

    /** 변화량 +1 강화 여부 */
    @Column(name = "change_amount_enhanced", nullable = false)
    private boolean changeAmountEnhanced;

    /** 타이밍 강화 상태 */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "timing_enhancement", nullable = false)
    private TimingEnhancement timingEnhancement;

    @Column(name = "acquired_at", nullable = false, updatable = false)
    private LocalDateTime acquiredAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 미강화(기본본) 인스턴스를 생성한다.
     *
     * @param userId 유저 ID
     * @param cardId 마스터 구종 ID
     */
    public UserPitchCard(Long userId, Long cardId) {
        this.userId = userId;
        this.cardId = cardId;
        this.changeAmountEnhanced = false;
        this.timingEnhancement = TimingEnhancement.NONE;
    }

    /**
     * 미강화 기본본인지.
     *
     * @return 강화가 하나도 없으면 true
     */
    public boolean isBaseCopy() {
        return !changeAmountEnhanced && timingEnhancement == TimingEnhancement.NONE;
    }

    /**
     * 핸드 슬롯 코스트를 반환한다 (1~3).
     *
     * @return 코스트
     */
    public int resolveCost() {
        int cost = 1;
        if (changeAmountEnhanced) {
            cost++;
        }
        if (timingEnhancement != TimingEnhancement.NONE) {
            cost++;
        }
        return cost;
    }

    /**
     * 강화 적용 횟수를 반환한다 (0~2).
     *
     * @return 강화 횟수
     */
    public int enhancementCount() {
        return resolveCost() - 1;
    }

    /**
     * 실효 변화량을 반환한다.
     *
     * @param baseChangeAmount 마스터 변화량
     * @return 실효 변화량
     */
    public int resolveEffectiveChangeAmount(int baseChangeAmount) {
        return changeAmountEnhanced ? baseChangeAmount + 1 : baseChangeAmount;
    }

    /**
     * 실효 타이밍을 반환한다.
     *
     * @param baseTiming 마스터 타이밍
     * @return 실효 타이밍
     */
    public Timing resolveEffectiveTiming(Timing baseTiming) {
        return switch (timingEnhancement) {
            case NONE -> baseTiming;
            case FASTER -> Timing.values()[baseTiming.ordinal() - 1];
            case SLOWER -> Timing.values()[baseTiming.ordinal() + 1];
        };
    }

    /**
     * 변화량 강화를 적용한다.
     */
    public void enhanceChangeAmount() {
        if (this.changeAmountEnhanced) {
            throw new IllegalStateException("이미 변화량 강화가 적용된 카드입니다.");
        }
        this.changeAmountEnhanced = true;
        touch();
    }

    /**
     * 타이밍 강화를 적용한다.
     *
     * @param enhancement FASTER 또는 SLOWER
     */
    public void enhanceTiming(TimingEnhancement enhancement) {
        if (enhancement == null || enhancement == TimingEnhancement.NONE) {
            throw new IllegalArgumentException("타이밍 강화 방향이 올바르지 않습니다.");
        }
        if (this.timingEnhancement != TimingEnhancement.NONE) {
            throw new IllegalStateException("이미 타이밍 강화가 적용된 카드입니다.");
        }
        this.timingEnhancement = enhancement;
        touch();
    }

    /**
     * 변화량 강화를 되돌린다.
     */
    public void revertChangeAmount() {
        if (!this.changeAmountEnhanced) {
            throw new IllegalStateException("변화량 강화가 적용되어 있지 않습니다.");
        }
        this.changeAmountEnhanced = false;
        touch();
    }

    /**
     * 타이밍 강화를 되돌린다.
     */
    public void revertTiming() {
        if (this.timingEnhancement == TimingEnhancement.NONE) {
            throw new IllegalStateException("타이밍 강화가 적용되어 있지 않습니다.");
        }
        this.timingEnhancement = TimingEnhancement.NONE;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(KST);
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now(KST);
        this.acquiredAt = now;
        this.updatedAt = now;
        if (this.timingEnhancement == null) {
            this.timingEnhancement = TimingEnhancement.NONE;
        }
    }
}
