package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.card.entity.CoordinateCard;
import com.project.bluffball.domain.card.enums.UserType;
import com.project.bluffball.domain.card.repository.CoordinateCardRepository;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 좌표 카드(CoordinateCard) DB 조회 전담 리더.
 */
@Component
@RequiredArgsConstructor
public class CoordinateCardReader {

    private final CoordinateCardRepository coordinateCardRepository;

    public int getPitcherStartCoordinateNumber(Long cardId) {
        CoordinateCard card = getById(cardId);
        if (card.getUserType() != UserType.PITCHER && card.getUserType() != UserType.BOTH) {
            throw new BadRequestException(ErrorCode.COORDINATE_CARD_INVALID_TYPE, "cardId=" + cardId);
        }
        if (card.getCoordinateNumber() == 0) {
            throw new BadRequestException(ErrorCode.GAME_INVALID_COORDINATE, "cardId=" + cardId);
        }
        return card.getCoordinateNumber();
    }

    public Long getCoordinateCardId(int coordinateNumber) {
        return coordinateCardRepository.findByCoordinateNumber(coordinateNumber)
                .map(CoordinateCard::getId)
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.COORDINATE_CARD_MASTER_NOT_FOUND,
                        "coordinateNumber=" + coordinateNumber));
    }

    public boolean isStrikeZone(int coordinateNumber) {
        if (coordinateNumber == 0) {
            return false;
        }
        return coordinateCardRepository.findByCoordinateNumber(coordinateNumber)
                .map(CoordinateCard::isStrike)
                .orElseThrow(() -> new BadRequestException(
                        ErrorCode.COORDINATE_CARD_MASTER_NOT_FOUND,
                        "coordinateNumber=" + coordinateNumber));
    }

    public CoordinateCard getById(Long cardId) {
        return coordinateCardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COORDINATE_CARD_NOT_FOUND, "cardId=" + cardId));
    }
}
