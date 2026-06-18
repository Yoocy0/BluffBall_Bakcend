package com.project.bluffball.domain.game.service.usecase.validator;

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
                         boolean mulliganDone) {
        if (!mulliganDone) {
            throw new IllegalStateException("카드 교체(멀리건) 확정 후에 투구할 수 있습니다.");
        }
        if (pitcherUserId == null || !pitcherUserId.equals(requestUserId)) {
            throw new IllegalArgumentException("현재 등판 투수만 카드를 선택할 수 있습니다.");
        }
        if (pitchCardId == null || coordinateCardId == null) {
            throw new IllegalArgumentException("구종 카드와 시작 좌표 카드를 모두 선택해야 합니다.");
        }
        if (!pitcherCardHand.contains(pitchCardId)) {
            throw new IllegalArgumentException(
                    "선택한 구종 카드가 현재 패에 없습니다. pitchCardId=" + pitchCardId);
        }
    }
}
