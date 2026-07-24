package com.project.bluffball.domain.team.service.usecase.validator;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 팀 창단 요청 값 검증 (usecase/validator 계층).
 *
 * <p>Repository·Entity에 접근하지 않고, 인자로 받은 원시 값만 검증한다.</p>
 */
@Component
public class TeamCreateValidator {

    private static final int MAX_NAME_LENGTH = 30;

    /**
     * 팀 이름 규칙을 검증한다.
     *
     * @param name 팀 이름
     * @throws BadRequestException 이름이 비어 있거나 길이 초과 시
     */
    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException(ErrorCode.TEAM_NAME_INVALID, "name is blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BadRequestException(
                    ErrorCode.TEAM_NAME_INVALID,
                    "name length=" + name.length() + ", max=" + MAX_NAME_LENGTH);
        }
    }

    /**
     * 팀 이름 중복 여부를 검증한다.
     *
     * @param nameExists {@code TeamReader.existsByName} 조회 결과
     * @throws ConflictException 이미 존재하는 이름이면
     */
    public void validateNameNotDuplicated(boolean nameExists) {
        if (nameExists) {
            throw new ConflictException(ErrorCode.TEAM_NAME_DUPLICATED);
        }
    }

    /**
     * 이미 다른 팀에 소속되어 있지 않은지 검증한다.
     *
     * @param alreadyJoined {@code TeamMemberReader.existsByUserId} 조회 결과
     * @throws ConflictException 이미 소속 팀이 있으면
     */
    public void validateNotAlreadyJoined(boolean alreadyJoined) {
        if (alreadyJoined) {
            throw new ConflictException(ErrorCode.TEAM_ALREADY_JOINED);
        }
    }
}
