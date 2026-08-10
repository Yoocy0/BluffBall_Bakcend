package com.project.bluffball.domain.game.dto.request;

import com.project.bluffball.domain.game.enums.BotDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Showdown 봇 매치 시작 요청.
 *
 * @param difficulty       봇 난이도
 * @param fromMatchmaking  매칭 대기 중 봇으로 전환했는지 — true면 큐 취소를 시도한다
 */
public record BotMatchStartRequest(
        @NotNull(message = "difficulty는 필수입니다.")
        @Schema(description = "봇 난이도", example = "NORMAL")
        BotDifficulty difficulty,

        @Schema(description = "매칭 큐 대기 중 봇 전환 여부", example = "false", defaultValue = "false")
        Boolean fromMatchmaking
) {

    /**
     * fromMatchmaking null-safe 조회.
     *
     * @return true이면 큐 대기에서 전환한 요청
     */
    public boolean isFromMatchmaking() {
        return Boolean.TRUE.equals(fromMatchmaking);
    }
}
