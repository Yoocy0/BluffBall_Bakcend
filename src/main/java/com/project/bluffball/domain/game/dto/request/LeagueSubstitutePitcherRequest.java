package com.project.bluffball.domain.game.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 리그 경기 투수 교체 WebSocket 요청 DTO.
 *
 * <p>교체 시 사전 지정 {@code dropCardId}가 핸드에서 빠지고 n장으로 등판한다.
 * Compact 최대 1회 / Full 최대 3회.</p>
 */
public record LeagueSubstitutePitcherRequest(

        /** 새로 등판할 투수 유저 ID (출전 로스터 내) */
        @NotNull
        Long newPitcherUserId
) {
}
