package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 투수 카드 선택 요청 검증 컴포넌트.
 */
@Component
public class PitcherCardSelectValidator {

    public void validate(List<Long> pitcherCardHand,
                         Long pitcherUserId,
                         Long requestUserId,
                         Long pitchCardId,
                         Long coordinateCardId,
                         boolean pitcherMulliganDone,
                         boolean pitcherAlreadySelected) {
        if (pitcherAlreadySelected) {
            throw new BadRequestException(ErrorCode.GAME_PITCHER_ALREADY_SELECTED);
        }
        if (!pitcherMulliganDone) {
            throw new BadRequestException(ErrorCode.GAME_MULLIGAN_REQUIRED);
        }
        if (pitcherUserId == null || !pitcherUserId.equals(requestUserId)) {
            throw new BadRequestException(ErrorCode.GAME_NOT_PITCHER);
        }
        if (pitchCardId == null || coordinateCardId == null) {
            throw new BadRequestException(ErrorCode.GAME_CARD_SELECTION_INCOMPLETE);
        }
        if (!pitcherCardHand.contains(pitchCardId)) {
            throw new BadRequestException(ErrorCode.GAME_CARD_NOT_IN_HAND, "pitchCardId=" + pitchCardId);
        }
    }
}
