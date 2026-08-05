package com.project.bluffball.domain.tutorial.dto.response;

/**
 * 시작 구종 선택지 (마스터 카드).
 *
 * @param cardId 마스터 구종 ID
 * @param name   구종 이름
 */
public record TutorialStarterPitchOption(
        Long cardId,
        String name
) {
}
