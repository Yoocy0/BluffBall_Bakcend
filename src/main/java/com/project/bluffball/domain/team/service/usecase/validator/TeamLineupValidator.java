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
     * 로스터 인원 수가 모드 요구와 일치하는지 검증한다.
     *
     * @param actualSize 요청 인원 수
     * @param requiredSize 모드별 필요 인원
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
     * 로스터에 중복 유저가 없는지 검증한다.
     *
     * @param userIds 출전 유저 ID 목록
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
}
