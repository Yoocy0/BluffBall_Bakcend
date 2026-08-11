package com.project.bluffball.gametest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 게임 테스트 개발자 PIN 로그인 요청.
 *
 * @param pin 4자리 개발자 PIN
 */
public record GameTestDevLoginRequest(
        @NotBlank(message = "pin은 필수입니다.")
        String pin
) {
}
