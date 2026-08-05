package com.project.bluffball.domain.tutorial.dto.response;

import com.project.bluffball.domain.card.dto.response.UserPitchCardResponse;

import java.util.List;

/**
 * 튜토리얼 완료 응답.
 *
 * @param grantedPitchCards 이번 완료로 지급·보유된 시작 구종 목록
 */
public record TutorialCompleteResponse(
        List<UserPitchCardResponse> grantedPitchCards
) {
}
