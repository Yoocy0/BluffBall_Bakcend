package com.project.bluffball.domain.card.entity;

import com.project.bluffball.domain.card.enums.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카드 도메인의 루트 엔터티.
 *
 * SINGLE_TABLE 전략 사용 — 모든 카드 타입이 단일 'card' 테이블에 저장된다.
 * card_type_code 컬럼 값:
 *   0 = 기본 Card (타이밍 카드, 구종 강화 카드)
 *   1 = PitchCard (구종 카드)
 *   2 = CoordinateCard (좌표 카드)
 */
@Entity
@Table(name = "card")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
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
