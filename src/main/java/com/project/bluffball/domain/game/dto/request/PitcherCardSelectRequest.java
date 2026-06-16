package com.project.bluffball.domain.game.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투수 카드 선택 WebSocket 수신 DTO.
 * 투수가 구종 카드와 시작 좌표를 선택할 때 클라이언트가 전송한다.
 */
@Getter
@NoArgsConstructor
public class PitcherCardSelectRequest {

    /** 선택한 구종 카드 ID */
    private Long pitchCardId;

    /** 선택한 시작 좌표 카드 ID */
    private Long coordinateCardId;
}
