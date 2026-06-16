package com.project.bluffball.domain.game.dto.response;

import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.game.enums.Timing;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투수 카드 패 내 개별 카드 정보 DTO.
 * {@link CardHandEvent}의 리스트 원소로 사용된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardInfo {

    /** 카드 ID */
    private Long cardId;

    /** 카드 이름 (구종명) */
    private String name;

    /** 좌표 변화량 */
    private int changeAmount;

    /** 변화 방향 — NONE(직구), SIDE(횡), DOWN(종) */
    private ChangeDirection direction;

    /**
     * 이 구종의 고유 타이밍.
     * 타자는 타격 화면의 5개 타이밍 칸 중 하나를 선택하며,
     * 이 값과 ordinal 차이가 0이면 주사위 2개, 1이면 주사위 1개(빗맞음), 2 이상이면 헛스윙이다.
     */
    private Timing timing;

    @Builder
    public CardInfo(Long cardId, String name, int changeAmount,
                    ChangeDirection direction, Timing timing) {
        this.cardId = cardId;
        this.name = name;
        this.changeAmount = changeAmount;
        this.direction = direction;
        this.timing = timing;
    }
}
