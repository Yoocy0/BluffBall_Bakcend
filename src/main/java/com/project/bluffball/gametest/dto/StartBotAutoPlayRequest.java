package com.project.bluffball.gametest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 봇 자동 플레이 시작 요청.
 *
 * @param matchSessionId 매치 세션
 * @param botUserIds 자동 조작할 봇 userId 목록
 */
public record StartBotAutoPlayRequest(
        @NotBlank String matchSessionId,
        @NotEmpty List<Long> botUserIds
) {
}
