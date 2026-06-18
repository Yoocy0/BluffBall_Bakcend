package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.card.entity.CoordinateCard;
import com.project.bluffball.domain.card.enums.UserType;
import com.project.bluffball.domain.card.repository.CoordinateCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 좌표 카드(CoordinateCard) DB 조회 전담 리더.
 */
@Component
@RequiredArgsConstructor
public class CoordinateCardReader {

    private final CoordinateCardRepository coordinateCardRepository;

    /**
     * 투수 시작 좌표 카드의 좌표 번호(1~25)를 반환한다. (Service ✅)
     */
    public int getPitcherStartCoordinateNumber(Long cardId) {
        CoordinateCard card = getById(cardId);
        if (card.getUserType() != UserType.PITCHER && card.getUserType() != UserType.BOTH) {
            throw new IllegalArgumentException(
                    "투수 시작 좌표 카드가 아닙니다. cardId=" + cardId);
        }
        return card.getCoordinateNumber();
    }

    /** Executor·Reader 내부 전용 — Service에서 호출 금지 */
    public CoordinateCard getById(Long cardId) {
        return coordinateCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "좌표 카드를 찾을 수 없습니다. cardId=" + cardId));
    }
}
