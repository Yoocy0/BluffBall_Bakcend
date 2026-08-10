package com.project.bluffball.domain.card.entity;

import com.project.bluffball.domain.card.enums.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카드 도메인의 루트 엔터티.
 *
 * <p>JOINED 전략 — 공통 필드는 {@code card} 테이블, 타입별 필드는 자식 테이블에 저장한다.</p>
 * <ul>
 *   <li>{@code card} — 공통 (name, user_type, card_type_code)</li>
 *   <li>{@code pitch_card} — 구종 전용 (change_amount, direction, timing)</li>
 *   <li>{@code coordinate_card} — 좌표 전용 (coordinate_number, is_strike)</li>
 *   <li>{@code enhancement_card} — 강화 전용 (effect)</li>
 * </ul>
 *
 * <p>{@code card_type_code} discriminator:</p>
 * <ul>
 *   <li>0 = 기본 Card</li>
 *   <li>1 = PitchCard</li>
 *   <li>2 = CoordinateCard</li>
 *   <li>3 = EnhancementCard</li>
 * </ul>
 */
@Entity
@Table(name = "card")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "card_type_code", discriminatorType = DiscriminatorType.INTEGER)
@DiscriminatorValue("0")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    public Card(String name, UserType userType) {
        this.name = name;
        this.userType = userType;
    }
}
