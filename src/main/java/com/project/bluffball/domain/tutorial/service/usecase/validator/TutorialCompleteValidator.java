package com.project.bluffball.domain.tutorial.service.usecase.validator;

import com.project.bluffball.domain.tutorial.TutorialConstants;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ConflictException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 튜토리얼 완료·선택 검증 (원시 값만 사용).
 */
@Component
public class TutorialCompleteValidator {

    /**
     * 이미 완료된 튜토리얼인지 검증한다.
     *
     * @param completed 완료 여부
     */
    public void validateNotCompleted(boolean completed) {
        if (completed) {
            throw new ConflictException(ErrorCode.TUTORIAL_ALREADY_COMPLETED);
        }
    }

    /**
     * 선택 구종 ID·이름이 시작 지급 규칙에 맞는지 검증한다.
     *
     * @param selectedCardIds   요청된 카드 ID 목록
     * @param selectedCardNames ID에 대응하는 구종 이름 (동일 순서)
     */
    public void validateStarterSelection(List<Long> selectedCardIds, List<String> selectedCardNames) {
        if (selectedCardIds == null
                || selectedCardIds.size() != TutorialConstants.SELECTABLE_STARTER_COUNT
                || selectedCardNames == null
                || selectedCardNames.size() != TutorialConstants.SELECTABLE_STARTER_COUNT) {
            throw new BadRequestException(
                    ErrorCode.TUTORIAL_STARTER_SELECTION_INVALID,
                    "커브·슬라이더·포크 중 정확히 2개를 선택해야 합니다.");
        }

        Set<Long> uniqueIds = new HashSet<>(selectedCardIds);
        if (uniqueIds.size() != selectedCardIds.size()) {
            throw new BadRequestException(
                    ErrorCode.TUTORIAL_STARTER_SELECTION_INVALID,
                    "동일한 구종을 중복 선택할 수 없습니다.");
        }

        for (String name : selectedCardNames) {
            if (!TutorialConstants.SELECTABLE_STARTER_PITCH_NAME_SET.contains(name)) {
                throw new BadRequestException(
                        ErrorCode.TUTORIAL_STARTER_SELECTION_INVALID,
                        "선택 가능 구종이 아닙니다: " + name);
            }
        }
    }
}
