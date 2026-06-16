package com.project.bluffball.domain.game.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 타자 카드 선택 WebSocket 수신 DTO.
 * 타자가 투수의 최종 좌표와 타이밍을 예측하여 선택할 때 클라이언트가 전송한다.
 */
@Getter
@NoArgsConstructor
public class BatterCardSelectRequest {

    /** 타자가 예측한 투수 공의 최종 좌표 카드 ID */
    private Long batterCoordinateCardId;

    /** 타자가 예측한 투수 공의 타이밍 카드 ID */
    private Long timingCardId;
}
