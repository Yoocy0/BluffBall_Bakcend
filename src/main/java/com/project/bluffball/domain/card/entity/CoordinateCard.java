package com.project.bluffball.domain.card.entity;

import com.project.bluffball.domain.card.enums.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좌표 카드 엔터티 (card_type_code = 2).
 * coordinateNumber: 유저 화면에 표시되는 1~25 사이의 고유 좌표 번호.
 * 내부 2D 인덱스(0~4, 0~4) 변환은 PitchCard 연산 레이어에서 처리한다.
 */
@Entity
@DiscriminatorValue("2")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoordinateCard extends Card {

    /** 유저 화면과 매핑되는 고유 좌표 번호 (1 ~ 25) */
    @Column(name = "coordinate_number", nullable = false)
    private int coordinateNumber;

    /** true = 스트라이크 존, false = 볼 존 */
    @Column(name = "is_strike", nullable = false)
    private boolean isStrike;

    public CoordinateCard(String name, UserType userType,
                            int coordinateNumber, boolean isStrike) {
        super(name, userType);
        this.coordinateNumber = coordinateNumber;
        this.isStrike = isStrike;
    }
}
