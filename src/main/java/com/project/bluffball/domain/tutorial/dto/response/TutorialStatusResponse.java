package com.project.bluffball.domain.tutorial.dto.response;

import java.util.List;

/**
 * 튜토리얼 진행 상태 응답.
 *
 * @param completed              완료 여부
 * @param fixedStarterPitch      고정 지급 구종 (포심)
 * @param selectableStarterPitches 선택 가능 구종 (커브·슬라이더·포크)
 * @param selectableCount        선택해야 하는 개수
 */
public record TutorialStatusResponse(
        boolean completed,
        TutorialStarterPitchOption fixedStarterPitch,
        List<TutorialStarterPitchOption> selectableStarterPitches,
        int selectableCount
) {
}
