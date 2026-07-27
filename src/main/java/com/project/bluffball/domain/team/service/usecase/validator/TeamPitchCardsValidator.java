package com.project.bluffball.domain.team.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 구종·강화 사전 선택 요청 값 검증 (usecase/validator 계층).
 */
@Component
public class TeamPitchCardsValidator {

    /**
     * 선택 멤버 집합이 로스터와 일치하는지 검증한다.
     *
     * @param selectionUserIds 선택 요청 유저 ID
     * @param lineupUserIds 로스터 유저 ID
     * @throws BadRequestException 집합이 다르면
     */
    public void validateMembersMatchLineup(List<Long> selectionUserIds, List<Long> lineupUserIds) {
        Set<Long> selectionSet = new HashSet<>(selectionUserIds);
        Set<Long> lineupSet = new HashSet<>(lineupUserIds);
        if (selectionUserIds.size() != selectionSet.size()) {
            throw new BadRequestException(
                    ErrorCode.TEAM_PITCH_CARDS_MEMBER_MISMATCH,
                    "duplicate selection userIds");
        }
        if (!selectionSet.equals(lineupSet)) {
            throw new BadRequestException(ErrorCode.TEAM_PITCH_CARDS_MEMBER_MISMATCH);
        }
    }

    /**
     * 카드 장수가 모드 핸드(n+1)와 일치하는지 검증한다.
     *
     * @param actualSize 선택 장수
     * @param requiredHandSize 필요 장수
     * @throws BadRequestException 장수 불일치 시
     */
    public void validateHandSize(int actualSize, int requiredHandSize) {
        if (actualSize != requiredHandSize) {
            throw new BadRequestException(
                    ErrorCode.TEAM_PITCH_CARDS_SIZE_INVALID,
                    "actual=" + actualSize + ", required=" + requiredHandSize);
        }
    }

    /**
     * dropCardId가 선택 카드에 포함되는지 검증한다.
     *
     * @param cardIds 선택 카드 ID
     * @param dropCardId 교체 시 제외 카드 ID
     * @throws BadRequestException 포함되지 않으면
     */
    public void validateDropCardInHand(List<Long> cardIds, Long dropCardId) {
        if (dropCardId == null || !cardIds.contains(dropCardId)) {
            throw new BadRequestException(
                    ErrorCode.TEAM_PITCH_DROP_CARD_INVALID,
                    "dropCardId=" + dropCardId);
        }
    }

    /**
     * 카드 ID가 구종·강화로 유효한지 검증한다.
     *
     * @param allValid {@code CardReader.areValidPitcherHandCards} 결과
     * @throws BadRequestException 유효하지 않으면
     */
    public void validateCardsValid(boolean allValid) {
        if (!allValid) {
            throw new BadRequestException(ErrorCode.TEAM_PITCH_CARDS_INVALID);
        }
    }
}
