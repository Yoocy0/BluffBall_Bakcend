package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * 멀리건(카드 교체) 요청 검증 컴포넌트.
 */
@Component
@RequiredArgsConstructor
public class MulliganValidator {

    private final GameModeRule gameModeRule;

    public void validateInGameMulliganSupported(GameMode gameMode) {
        if (!gameModeRule.usesInGameCardDrawAndMulligan(gameMode)) {
            throw new BadRequestException(ErrorCode.GAME_INGAME_MULLIGAN_UNSUPPORTED);
        }
    }

    public void validateMulliganAllowedForUser(boolean mulliganDoneForUser) {
        if (mulliganDoneForUser) {
            throw new BadRequestException(ErrorCode.GAME_MULLIGAN_ALREADY_USED);
        }
    }

    public void validateParticipant(MatchInfoReader matchInfoReader,
                                    String matchSessionId,
                                    Long requestUserId) {
        if (!matchInfoReader.isParticipant(matchSessionId, requestUserId)) {
            throw new ForbiddenException(ErrorCode.MATCH_NOT_PARTICIPANT);
        }
    }

    public void validate(List<Long> currentHand, List<Long> cardIdsToSwap) {
        if (cardIdsToSwap == null || cardIdsToSwap.isEmpty()) {
            return;
        }

        if (cardIdsToSwap.size() > currentHand.size()) {
            throw new BadRequestException(
                    ErrorCode.GAME_MULLIGAN_INVALID,
                    "교체 카드 수(" + cardIdsToSwap.size() + ")가 현재 패 크기(" + currentHand.size() + ")를 초과합니다.");
        }

        if (new HashSet<>(cardIdsToSwap).size() != cardIdsToSwap.size()) {
            throw new BadRequestException(ErrorCode.GAME_MULLIGAN_INVALID, "교체 목록에 중복된 카드 ID가 있습니다.");
        }

        HashSet<Long> handSet = new HashSet<>(currentHand);
        for (Long id : cardIdsToSwap) {
            if (!handSet.contains(id)) {
                throw new BadRequestException(
                        ErrorCode.GAME_MULLIGAN_INVALID, "교체하려는 카드가 현재 패에 없습니다. cardId=" + id);
            }
        }
    }
}
