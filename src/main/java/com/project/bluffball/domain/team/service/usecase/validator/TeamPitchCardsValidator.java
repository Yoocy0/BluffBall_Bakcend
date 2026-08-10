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
     * dropCardId가 선택 인스턴스에 포함되는지 검증한다.
     *
     * @param userPitchCardIds 선택 인스턴스 ID
     * @param dropCardId       교체 시 제외 인스턴스 ID
     * @throws BadRequestException 포함되지 않으면
     */
    public void validateDropCardInHand(List<Long> userPitchCardIds, Long dropCardId) {
        if (dropCardId == null || !userPitchCardIds.contains(dropCardId)) {
            throw new BadRequestException(
                    ErrorCode.TEAM_PITCH_DROP_CARD_INVALID,
                    "dropCardId=" + dropCardId);
        }
    }

    /**
     * 인스턴스 ID가 구종 로드아웃으로 유효한지 검증한다.
     *
     * @param allValid {@code UserPitchCardReader#areValidPitchInstances} 결과
     * @throws BadRequestException 유효하지 않으면
     */
    public void validateCardsValid(boolean allValid) {
        if (!allValid) {
            throw new BadRequestException(ErrorCode.TEAM_PITCH_CARDS_INVALID);
        }
    }

    /**
     * 요청자가 출전 로스터에 포함되는지 검증한다.
     *
     * @param requesterUserId 요청자
     * @param matchRosterUserIds 로스터
     * @throws BadRequestException 로스터에 없으면
     */
    public void validateRequesterInRoster(Long requesterUserId, List<Long> matchRosterUserIds) {
        if (requesterUserId == null || !matchRosterUserIds.contains(requesterUserId)) {
            throw new BadRequestException(
                    ErrorCode.TEAM_PITCH_CARDS_MEMBER_MISMATCH,
                    "requester not in roster userId=" + requesterUserId);
        }
    }

    /**
     * 보유 여부를 검증한다.
     *
     * @param ownsAll 전부 보유 여부
     * @throws BadRequestException 미보유 포함 시
     */
    public void validateOwned(boolean ownsAll) {
        if (!ownsAll) {
            throw new BadRequestException(ErrorCode.TEAM_PITCH_CARDS_NOT_OWNED);
        }
    }

    /**
     * 코스트 합이 핸드 장수와 같은지 검증한다.
     *
     * @param totalCost 코스트 합
     * @param requiredHandSize 모드 핸드 장수
     * @throws BadRequestException 불일치 시
     */
    public void validateTotalCost(int totalCost, int requiredHandSize) {
        if (totalCost != requiredHandSize) {
            throw new BadRequestException(
                    ErrorCode.TEAM_PITCH_CARDS_COST_INVALID,
                    "totalCost=" + totalCost + ", required=" + requiredHandSize);
        }
    }
}
