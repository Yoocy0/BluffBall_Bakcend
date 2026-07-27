package com.project.bluffball.domain.team.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 리그 출전 멤버별 구종·강화 카드 사전 선택 요청 DTO.
 *
 * <p>장수 = 모드 핸드(n+1). Compact 4 / Full 5.
 * {@code dropCardId}는 투수 교체 시 빠지는 +1장이다.</p>
 */
public record UpsertTeamPitchCardsRequest(

        /** 멤버별 사전 선택 목록 */
        @NotEmpty
        List<@Valid @NotNull MemberPitchCardSelection> selections
) {

    /**
     * 멤버 1명의 구종·강화 사전 선택.
     *
     * @param userId 출전 멤버 유저 ID
     * @param cardIds 구종·강화 카드 ID (n+1장)
     * @param dropCardId 교체 시 제외할 카드 ID ({@code cardIds}에 포함)
     */
    public record MemberPitchCardSelection(
            @NotNull Long userId,
            @NotEmpty List<@NotNull Long> cardIds,
            @NotNull Long dropCardId
    ) {
    }
}
