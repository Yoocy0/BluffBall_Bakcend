package com.project.bluffball.domain.game.dto.response;

import java.util.List;

/**
 * 투수 카드 패 전달 WebSocket 송신 이벤트.
 */
public record CardHandEvent(
        /** 투수에게 지급된 카드 패 목록 */
        List<CardInfo> cards
) {
}
