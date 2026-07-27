package com.project.bluffball.domain.league.service.usecase.validator;

import com.project.bluffball.domain.league.config.LeagueTierRule;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 티어 진입·승급 검증.
 */
@Component
public class LeagueProgressValidator {

    /**
     * 아직 해당 포맷 진행 상태가 없는지 검증한다 (최초 진입).
     *
     * @param alreadyExists 이미 존재하는지
     */
    public void validateNotAlreadyEntered(boolean alreadyExists) {
        if (alreadyExists) {
            throw new ConflictException(ErrorCode.LEAGUE_ALREADY_JOINED);
        }
    }

    /**
     * 최초 진입은 최하위 티어만 허용한다.
     *
     * @param tier 요청 티어
     */
    public void validateInitialTier(LeagueTier tier) {
        if (tier != LeagueTierRule.lowestTier()) {
            throw new BadRequestException(
                    ErrorCode.LEAGUE_TIER_ENTRY_INVALID,
                    "최초 진입은 " + LeagueTierRule.lowestTier() + " 만 가능합니다.");
        }
    }

    /**
     * 승급 자격을 검증한다.
     *
     * @param promoteReady 상한 도달 여부
     * @param currentTier 현재 티어
     * @param targetTier 목표 티어
     */
    public void validatePromote(boolean promoteReady, LeagueTier currentTier, LeagueTier targetTier) {
        LeagueTier expected = LeagueTierRule.nextTier(currentTier);
        if (expected == null || expected != targetTier) {
            throw new BadRequestException(
                    ErrorCode.LEAGUE_TIER_ENTRY_INVALID,
                    "current=" + currentTier + ", target=" + targetTier);
        }
        if (!promoteReady) {
            throw new BadRequestException(ErrorCode.LEAGUE_PROMOTE_NOT_READY);
        }
    }

    /**
     * 매칭 티어가 현재 소속과 같은지 검증한다.
     *
     * @param isCurrentTier 일치 여부
     */
    public void validateMatchingTier(boolean isCurrentTier) {
        if (!isCurrentTier) {
            throw new BadRequestException(ErrorCode.LEAGUE_MATCH_TIER_MISMATCH);
        }
    }

    /**
     * 참가비 충분 여부.
     *
     * @param treasury 금고
     * @param entryFee 참가비
     */
    public void validateSufficientTreasury(long treasury, long entryFee) {
        if (treasury < entryFee) {
            throw new BadRequestException(
                    ErrorCode.LEAGUE_TREASURY_INSUFFICIENT,
                    "treasury=" + treasury + ", entryFee=" + entryFee);
        }
    }

    /**
     * 최소 팀원 수를 검증한다.
     *
     * @param memberCount 현재 팀원 수
     * @param minTeamMembers 최소 필요 인원
     */
    public void validateMemberCount(long memberCount, int minTeamMembers) {
        if (memberCount < minTeamMembers) {
            throw new BadRequestException(
                    ErrorCode.LEAGUE_MEMBERS_INSUFFICIENT,
                    "memberCount=" + memberCount + ", min=" + minTeamMembers);
        }
    }
}
