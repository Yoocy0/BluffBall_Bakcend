package com.project.bluffball.gametest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 테스트용 리그 투수 교체 요청.
 *
 * @param matchSessionId 매치 세션
 * @param newPitcherUserId 신임 투수
 */
public record TestSubstitutePitcherRequest(
        @NotBlank String matchSessionId,
        @NotNull Long newPitcherUserId
) {
}
