package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
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
                         boolean pitcherSelectionComplete,
                         boolean batterAlreadySelected) {
        if (batterAlreadySelected) {
            throw new BadRequestException(ErrorCode.GAME_BATTER_ALREADY_SELECTED);
        }
        if (!pitcherSelectionComplete) {
            throw new BadRequestException(ErrorCode.GAME_PITCHER_SELECTION_REQUIRED);
        }
        if (batterUserId == null || !batterUserId.equals(requestUserId)) {
            throw new BadRequestException(ErrorCode.GAME_NOT_BATTER);
        }
        if (responseTimeSec < 0) {
            throw new BadRequestException(ErrorCode.GAME_INVALID_RESPONSE_TIME);
        }
        if (responseTimeSec <= MAX_RESPONSE_TIME_SEC) {
            if (timing == null) {
                throw new BadRequestException(ErrorCode.GAME_INVALID_TIMING);
            }
            if (batterCoordinateNumber < 0 || batterCoordinateNumber > 25) {
                throw new BadRequestException(
                        ErrorCode.GAME_INVALID_COORDINATE, "coordinate=" + batterCoordinateNumber);
            }
        }
    }
}
