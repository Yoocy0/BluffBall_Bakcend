package com.project.bluffball.domain.game.dto.response;

import com.project.bluffball.domain.card.enums.ChangeDirection;
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

    /** 이 구종에 연결된 타이밍 카드 ID */
    private Long timingCardId;

    /** 이 구종의 타이밍 카드 이름 (예: "빠름", "중간", "느림") */
    private String timingName;

    @Builder
    public CardInfo(Long cardId, String name, int changeAmount, ChangeDirection direction,
                    Long timingCardId, String timingName) {
        this.cardId = cardId;
        this.name = name;
        this.changeAmount = changeAmount;
        this.direction = direction;
        this.timingCardId = timingCardId;
        this.timingName = timingName;
    }
}
