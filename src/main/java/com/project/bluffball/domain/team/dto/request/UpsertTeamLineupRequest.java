package com.project.bluffball.domain.team.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 리그 출전 로스터 저장 요청 DTO.
 *
 * <p>Compact: 타순 3명 + 전담 투수 1명(userIds에 포함하지 않음).
 * Full: 타순 9명, 선발 투수는 userIds에 포함.
 * {@code userIds} 순서 = 타순.</p>
 */
public record UpsertTeamLineupRequest(

        /** 타순 유저 ID 목록 (순서 = 타순) */
        @NotEmpty
        List<@NotNull Long> userIds,

        /** 선발 투수 유저 ID (Compact는 타순 밖, Full은 타순에 포함) */
        @NotNull
        Long startingPitcherUserId
) {
}
