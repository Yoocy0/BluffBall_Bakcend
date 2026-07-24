package com.project.bluffball.domain.team.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 리그 출전 로스터 저장 요청 DTO.
 *
 * <p>Compact 3명 / Full 9명. 전원 팀 소속 멤버여야 한다.</p>
 */
public record UpsertTeamLineupRequest(

        /** 출전 유저 ID 목록 (순서 = 기본 타순 후보) */
        @NotEmpty
        List<@NotNull Long> userIds
) {
}
