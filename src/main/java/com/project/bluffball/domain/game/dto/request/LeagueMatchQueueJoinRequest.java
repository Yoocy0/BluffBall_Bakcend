package com.project.bluffball.domain.game.dto.request;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import jakarta.validation.constraints.NotNull;

/**
 * 리그 매칭 큐 진입 요청 DTO.
 *
 * <p>시즌과 무관하게 {@code format × tier} 단위로 매칭한다.</p>
 */
public record LeagueMatchQueueJoinRequest(

        /** 리그 구분 (FULL / COMPACT) */
        @NotNull
        LeagueFormat format,

        /** 리그 단계 (AMATEUR_1 ~ PRO_2) */
        @NotNull
        LeagueTier tier
) {
}
