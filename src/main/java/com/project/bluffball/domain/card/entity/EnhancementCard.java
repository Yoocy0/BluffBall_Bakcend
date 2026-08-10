package com.project.bluffball.domain.card.entity;

import com.project.bluffball.domain.card.enums.EnhancementEffect;
import com.project.bluffball.domain.card.enums.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구종 강화 카드 마스터 (card_type_code = 3).
 *
 * <p>{@code enhancement_card} 테이블 — {@link Card}와 {@code card_id}로 JOIN.
 * 효과는 {@link EnhancementEffect}로 구분하며 id 하드코딩 분기는 하지 않는다.</p>
 */
@Entity
@Table(name = "enhancement_card")
@DiscriminatorValue("3")
@PrimaryKeyJoinColumn(name = "card_id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnhancementCard extends Card {

    /** 강화 효과 */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "effect", nullable = false, unique = true)
    private EnhancementEffect effect;

    /**
     * 강화 카드 마스터를 생성한다.
     *
     * @param name   카드 이름
     * @param effect 효과
     */
    public EnhancementCard(String name, EnhancementEffect effect) {
        super(name, UserType.PITCHER);
        this.effect = effect;
    }
}
