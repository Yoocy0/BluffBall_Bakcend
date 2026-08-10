package com.project.bluffball.domain.team.dto.response;

import com.project.bluffball.domain.league.enums.LeagueFormat;

import java.util.List;

/**
 * 리그 출전 멤버별 구종 사전 선택 응답 DTO.
 */
public record TeamPitchCardsResponse(
        Long teamId,
        LeagueFormat format,
        List<MemberPitchCards> selections
) {

    /**
     * 멤버 1명의 사전 선택.
     *
     * @param userId            유저 ID
     * @param userPitchCardIds  구종 인스턴스 ID (n+1)
     * @param dropCardId        교체 시 제외 인스턴스 ID
     */
    public record MemberPitchCards(
            Long userId,
            List<Long> userPitchCardIds,
            Long dropCardId
    ) {
    }
}
