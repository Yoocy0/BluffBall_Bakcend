package com.project.bluffball.domain.game.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 투수 카드 패 전달 WebSocket 송신 이벤트.
 * 경기 시작 또는 투수 교체 시 해당 투수에게만 전송된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardHandEvent {

    /** 투수에게 지급된 카드 패 목록 (5장) */
    private List<CardInfo> cards;

    @Builder
    public CardHandEvent(List<CardInfo> cards) {
        this.cards = cards;
    }
}
