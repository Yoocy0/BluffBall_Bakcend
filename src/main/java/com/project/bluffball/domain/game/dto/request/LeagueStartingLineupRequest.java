package com.project.bluffball.domain.game.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 리그 경기 선발 투수·타순 제출 WebSocket 요청 DTO.
 */
public record LeagueStartingLineupRequest(

        /** 선발 투수 유저 ID (출전 로스터 내) */
        @NotNull
        Long startingPitcherUserId,

        /** 타순 (출전 로스터 유저 ID, Compact 3 / Full 9) */
        @NotEmpty
        List<@NotNull Long> battingOrder
) {
}
