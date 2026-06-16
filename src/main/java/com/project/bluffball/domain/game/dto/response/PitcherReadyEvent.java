package com.project.bluffball.domain.game.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투수 카드 선택 완료 WebSocket 송신 이벤트.
 * 투수 선택 완료 시 타자에게만 전송된다.
 * 최종 좌표는 노출하지 않으며, 타자는 시작 좌표를 보고 최종 좌표와 타이밍을 예측한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PitcherReadyEvent {

    /** 투수가 선택한 시작 좌표 번호 (1~25) */
    private int startCoordinateNumber;

    @Builder
    public PitcherReadyEvent(int startCoordinateNumber) {
        this.startCoordinateNumber = startCoordinateNumber;
    }
}
