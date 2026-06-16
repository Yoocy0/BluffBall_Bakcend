package com.project.bluffball.domain.game.dto.request;

import com.project.bluffball.domain.game.enums.Timing;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 타자 카드 선택 WebSocket 수신 DTO.
 *
 * <p>투수의 {@code PitcherReadyEvent} 수신 후 5초 타이머 내에
 * 타자가 최종 좌표와 타이밍을 예측하여 선택할 때 클라이언트가 전송한다.</p>
 *
 * <p>5초 타임아웃 시 클라이언트는 {@code isTimeout=true}와 함께
 * {@code batterCoordinateCardId=null}, {@code timingCardId=null}로 전송한다.
 * 서버는 타임아웃 상태를 스윙 미발동으로 처리하며,
 * 투수 공의 최종 좌표가 스트라이크 존이면 스트라이크, 볼 존이면 볼로 판정한다.</p>
 */
@Getter
@NoArgsConstructor
public class BatterCardSelectRequest {

    /** 타자가 예측한 투수 공의 최종 좌표 카드 ID. 타임아웃 시 null. */
    private Long batterCoordinateCardId;

    /**
     * 타자가 선택한 타이밍 칸.
     * 타격 화면의 5개 타이밍 슬롯(TOO_EARLY ~ TOO_LATE) 중 하나를 선택한다.
     * 투수 구종 카드의 {@code timing} 값과 ordinal 차이로 주사위 개수가 결정된다.
     * 타임아웃 시 null.
     */
    private Timing timing;

    /**
     * 5초 타임아웃 여부.
     * {@code true}이면 스윙 미발동으로 간주하며, 좌표/타이밍 필드는 무시된다.
     */
    private boolean isTimeout;
}
