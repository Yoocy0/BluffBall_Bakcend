package com.project.bluffball.domain.game.dto.request;

import com.project.bluffball.domain.game.enums.Timing;

/**
 * 타자 카드 선택 WebSocket 수신 DTO.
 *
 * <p>투수의 {@code PitcherReadyEvent} 수신 후 5초 타이머 내에
 * 타자가 최종 좌표와 타이밍을 예측하여 선택할 때 클라이언트가 전송한다.</p>
 */
public record BatterCardSelectRequest(
        /** PitcherReadyEvent 수신 후 선택까지 경과 시간(초). 5초 초과 = 스윙 미발동 */
        double responseTimeSec,
        /** 타자가 예측한 투수 공의 최종 좌표 (1~25, 폭투 존 0) */
        int batterCoordinateNumber,
        /** 타자가 선택한 타이밍 칸 */
        Timing timing
) {
}
