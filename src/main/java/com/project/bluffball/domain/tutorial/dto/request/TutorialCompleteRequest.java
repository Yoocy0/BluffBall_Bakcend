package com.project.bluffball.domain.tutorial.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 튜토리얼 완료·시작 구종 선택 요청.
 *
 * @param selectedPitchCardIds 커브·슬라이더·포크 중 고른 마스터 구종 ID 2개
 */
public record TutorialCompleteRequest(
        @NotNull
        @Size(min = 2, max = 2)
        List<Long> selectedPitchCardIds
) {
}
