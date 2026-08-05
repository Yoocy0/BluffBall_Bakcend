package com.project.bluffball.domain.team.dto.response;

/**
 * 순위·목록 표시용 팀 요약 정보.
 */
public record TeamDisplayInfo(
        Long teamId,
        String name,
        String logoUrl
) {
}
