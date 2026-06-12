package com.project.bluffball.domain.card.entity;

import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.card.enums.UserType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 구종 카드 엔터티 (card_type_code = 1).
 *
 * <p>변화구 연산은 유저 좌표(1~25)를 내부 2D 인덱스(0~4, 0~4)로 변환 후 처리한다.
 * 격자 경계(인덱스 > 4)를 벗어나면 규격 외(폭투 등)로 판단하여 0을 반환한다.</p>
 *
 * <pre>
 * 좌표 번호 ↔ 2D 인덱스 변환 공식
 *   Y = (coordinateNumber - 1) / 5   (행, 0 = 상단)
 *   X = (coordinateNumber - 1) % 5   (열, 0 = 좌측)
 *   coordinateNumber = Y * 5 + X + 1
 * </pre>
 */
@Entity
@DiscriminatorValue("1")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PitchCard extends Card {

    /** 구종의 좌표 변화량 (정수) */
    @Column(name = "change_amount", nullable = false)
    private int changeAmount;

    /** 구종의 변화 방향 — DB에 ordinal(0=NONE, 1=DOWN, 2=SIDE) 정수로 저장 */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "direction", nullable = false)
    private ChangeDirection direction;

    /** 이 구종 카드가 참조하는 타이밍 카드의 ID */
    @Column(name = "timing_card_id")
    private Long timingCardId;

    /** userType 기본값 PITCHER */
    public PitchCard(String name, int changeAmount, ChangeDirection direction, Long timingCardId) {
        super(name, UserType.PITCHER);
        this.changeAmount = changeAmount;
        this.direction = direction;
        this.timingCardId = timingCardId;
    }

    public PitchCard(String name, UserType userType,
                        int changeAmount, ChangeDirection direction, Long timingCardId) {
        super(name, userType);
        this.changeAmount = changeAmount;
        this.direction = direction;
        this.timingCardId = timingCardId;
    }

    /**
     * 시작 좌표 번호에 이 구종 카드의 변화를 적용하여 최종 좌표 번호를 반환한다.
     *
     * @param startCoordinateNumber 유저 화면 기준 시작 좌표 번호 (1 ~ 25)
     * @return 변화 적용 후 최종 좌표 번호 (1 ~ 25), 격자 이탈 시 0
     */
    public int calculateFinalCoordinateNumber(int startCoordinateNumber) {
        int startY = (startCoordinateNumber - 1) / 5;
        int startX = (startCoordinateNumber - 1) % 5;

        int finalX = startX;
        int finalY = startY;

        switch (direction) {
            case SIDE -> {
                finalX = startX + this.changeAmount;
                if (finalX > 4) return 0;
            }
            case DOWN -> {
                finalY = startY + this.changeAmount;
                if (finalY > 4) return 0;
            }
            case NONE -> { /* 직구 계열 — 좌표 변동 없음 */ }
        }

        return (finalY * 5) + finalX + 1;
    }
}
