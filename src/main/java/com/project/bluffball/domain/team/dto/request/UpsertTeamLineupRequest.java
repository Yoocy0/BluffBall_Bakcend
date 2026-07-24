package com.project.bluffball.domain.team.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 리그 출전 로스터 저장 요청 DTO.
 *
 * <p>Compact 3명 / Full 9명.
 * {@code userIds} 순서 = 타순, {@code startingPitcherUserId} = 선발 투수(로스터에 포함).</p>
 */
public record UpsertTeamLineupRequest(

        /** 출전 유저 ID 목록 (순서 = 타순) */
        @NotEmpty
        List<@NotNull Long> userIds,

        /** 선발 투수 유저 ID (userIds에 포함) */
        @NotNull
        Long startingPitcherUserId
) {
}
