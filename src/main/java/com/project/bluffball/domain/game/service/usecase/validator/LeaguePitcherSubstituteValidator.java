package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 리그 투수 교체 검증 (usecase/validator).
 */
@Component
public class LeaguePitcherSubstituteValidator {

    /**
     * 리그 모드에서만 교체 가능함을 검증한다.
     *
     * @param maxSubstitutions 모드별 최대 교체 횟수
     * @throws BadRequestException 교체 불가 모드
     */
    public void validateSubstitutionSupported(int maxSubstitutions) {
        if (maxSubstitutions <= 0) {
            throw new BadRequestException(ErrorCode.GAME_PITCHER_SUBSTITUTE_UNSUPPORTED);
        }
    }

    /**
     * 교체 한도를 검증한다.
     *
     * @param usedCount 이미 사용한 교체 횟수
     * @param maxSubstitutions 최대 횟수
     * @throws BadRequestException 한도 초과
     */
    public void validateLimit(int usedCount, int maxSubstitutions) {
        if (usedCount >= maxSubstitutions) {
            throw new BadRequestException(
                    ErrorCode.GAME_PITCHER_SUBSTITUTE_LIMIT,
                    "used=" + usedCount + ", max=" + maxSubstitutions);
        }
    }

    /**
     * 요청자가 현재 투수(또는 수비 측 리더 등)인지 — 현재는 등판 투수만 허용.
     *
     * @param requestUserId 요청자
     * @param pitcherUserId 현재 투수
     * @throws BadRequestException 권한 없음
     */
    public void validateRequesterIsPitcher(Long requestUserId, Long pitcherUserId) {
        if (pitcherUserId == null || !pitcherUserId.equals(requestUserId)) {
            throw new BadRequestException(ErrorCode.GAME_NOT_PITCHER);
        }
    }

    /**
     * 신임 투수가 같은 수비 로스터·타순에 있고, 현재 투수·이미 등판한 투수가 아닌지 검증한다.
     *
     * @param newPitcherUserId 신임 투수
     * @param currentPitcherUserId 현재 투수
     * @param defendingRoster 수비 로스터
     * @param defendingBattingOrder 수비 팀 타순 (슬롯 교환 대상)
     * @param usedAsPitcherIds 이미 등판한 투수
     * @throws BadRequestException 대상 부적절
     */
    public void validateNewPitcher(
            Long newPitcherUserId,
            Long currentPitcherUserId,
            List<Long> defendingRoster,
            List<Long> defendingBattingOrder,
            List<Long> usedAsPitcherIds) {
        if (newPitcherUserId == null || newPitcherUserId.equals(currentPitcherUserId)) {
            throw new BadRequestException(ErrorCode.GAME_PITCHER_SUBSTITUTE_INVALID, "same pitcher");
        }
        if (defendingRoster == null || !defendingRoster.contains(newPitcherUserId)) {
            throw new BadRequestException(
                    ErrorCode.GAME_PITCHER_SUBSTITUTE_INVALID, "not on defending roster");
        }
        if (defendingBattingOrder == null || !defendingBattingOrder.contains(newPitcherUserId)) {
            throw new BadRequestException(
                    ErrorCode.GAME_PITCHER_SUBSTITUTE_INVALID, "not in batting order");
        }
        if (usedAsPitcherIds != null && usedAsPitcherIds.contains(newPitcherUserId)) {
            throw new BadRequestException(
                    ErrorCode.GAME_PITCHER_SUBSTITUTE_INVALID, "already used as pitcher");
        }
    }

    /**
     * @deprecated {@link #validateNewPitcher(Long, Long, List, List, List)} 사용
     */
    @Deprecated
    public void validateNewPitcher(
            Long newPitcherUserId,
            Long currentPitcherUserId,
            List<Long> defendingRoster,
            List<Long> usedAsPitcherIds) {
        validateNewPitcher(
                newPitcherUserId,
                currentPitcherUserId,
                defendingRoster,
                defendingRoster,
                usedAsPitcherIds);
    }

    /**
     * dropCardId 존재를 검증한다.
     *
     * @param dropCardId 교체 시 제외 카드
     * @throws BadRequestException 없음
     */
    public void validateDropCardPresent(Long dropCardId) {
        if (dropCardId == null) {
            throw new BadRequestException(ErrorCode.GAME_PITCHER_SUBSTITUTE_DROP_CARD_MISSING);
        }
    }
}
