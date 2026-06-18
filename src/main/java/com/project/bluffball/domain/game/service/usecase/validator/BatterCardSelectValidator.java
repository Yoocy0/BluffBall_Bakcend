package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.enums.Timing;
import org.springframework.stereotype.Component;

/**
 * 타자 카드 선택 요청 검증 컴포넌트.
 */
@Component
public class BatterCardSelectValidator {

    private static final double MAX_RESPONSE_TIME_SEC = 5.0;

    public void validate(Long batterUserId,
                         Long requestUserId,
                         double responseTimeSec,
                         int batterCoordinateNumber,
                         Timing timing,
                         boolean pitcherSelectionComplete) {
        if (!pitcherSelectionComplete) {
            throw new IllegalStateException("투수의 카드 선택이 완료되지 않았습니다.");
        }
        if (batterUserId == null || !batterUserId.equals(requestUserId)) {
            throw new IllegalArgumentException("현재 타석 타자만 선택할 수 있습니다.");
        }
        if (responseTimeSec < 0) {
            throw new IllegalArgumentException("응답 시간은 0 이상이어야 합니다.");
        }
        if (responseTimeSec <= MAX_RESPONSE_TIME_SEC) {
            if (timing == null) {
                throw new IllegalArgumentException("5초 이내 응답 시 타이밍을 선택해야 합니다.");
            }
            if (batterCoordinateNumber < 0 || batterCoordinateNumber > 25) {
                throw new IllegalArgumentException(
                        "좌표는 0(폭투 존) 또는 1~25 사이여야 합니다. coordinate=" + batterCoordinateNumber);
            }
        }
    }
}
