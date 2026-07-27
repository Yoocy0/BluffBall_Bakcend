package com.project.bluffball.domain.team.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 출전 로스터 요청 값 검증 (usecase/validator 계층).
 */
@Component
public class TeamLineupValidator {

    /**
     * 타순 인원 수가 모드 요구와 일치하는지 검증한다.
     *
     * @param actualSize 요청 타순 인원 수
     * @param requiredSize 모드별 필요 타순 인원
     * @throws BadRequestException 인원 수 불일치 시
     */
    public void validateSize(int actualSize, int requiredSize) {
        if (actualSize != requiredSize) {
            throw new BadRequestException(
                    ErrorCode.TEAM_LINEUP_SIZE_INVALID,
                    "actual=" + actualSize + ", required=" + requiredSize);
        }
    }

    /**
     * 타순에 중복 유저가 없는지 검증한다.
     *
     * @param userIds 타순 유저 ID 목록
     * @throws BadRequestException 중복이 있으면
     */
    public void validateNoDuplicates(List<Long> userIds) {
        Set<Long> unique = new HashSet<>(userIds);
        if (unique.size() != userIds.size()) {
            throw new BadRequestException(ErrorCode.TEAM_LINEUP_DUPLICATE_MEMBER);
        }
    }

    /**
     * 전원 팀 멤버인지 검증한다.
     *
     * @param allMembers true면 전원 멤버
     * @throws BadRequestException 멤버가 아닌 유저가 있으면
     */
    public void validateAllMembers(boolean allMembers) {
        if (!allMembers) {
            throw new BadRequestException(ErrorCode.TEAM_LINEUP_MEMBER_INVALID);
        }
    }

    /**
     * Full — 선발 투수가 타순에 포함되는지 검증한다.
     *
     * @param userIds 타순
     * @param startingPitcherUserId 선발 투수
     * @throws BadRequestException 포함되지 않으면
     */
    public void validateStartingPitcherInRoster(List<Long> userIds, Long startingPitcherUserId) {
        if (startingPitcherUserId == null || !userIds.contains(startingPitcherUserId)) {
            throw new BadRequestException(
                    ErrorCode.TEAM_LINEUP_STARTING_PITCHER_INVALID,
                    "startingPitcherUserId=" + startingPitcherUserId);
        }
    }

    /**
     * Compact — 선발 투수가 타순과 별도 인원인지 검증한다.
     *
     * @param battingOrderUserIds 타순
     * @param startingPitcherUserId 전담 선발 투수
     * @throws BadRequestException 없거나 타순에 포함되면
     */
    public void validateDedicatedStartingPitcher(
            List<Long> battingOrderUserIds,
            Long startingPitcherUserId) {
        if (startingPitcherUserId == null) {
            throw new BadRequestException(
                    ErrorCode.TEAM_LINEUP_STARTING_PITCHER_INVALID,
                    "startingPitcherUserId=null");
        }
        if (battingOrderUserIds.contains(startingPitcherUserId)) {
            throw new BadRequestException(
                    ErrorCode.TEAM_LINEUP_STARTING_PITCHER_INVALID,
                    "Compact starting pitcher must be outside batting order. startingPitcherUserId="
                            + startingPitcherUserId);
        }
    }

    /**
     * 선발 투수가 지정되어 있는지 검증한다. (매칭 진입용)
     *
     * @param hasStartingPitcher 선발 투수 존재 여부
     * @throws BadRequestException 없으면
     */
    public void validateStartingPitcherPresent(boolean hasStartingPitcher) {
        if (!hasStartingPitcher) {
            throw new BadRequestException(ErrorCode.TEAM_LINEUP_STARTING_PITCHER_INVALID);
        }
    }
}
