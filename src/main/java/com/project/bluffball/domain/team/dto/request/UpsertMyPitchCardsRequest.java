package com.project.bluffball.domain.team.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 본인 구종 사전 선택 요청 (리그 로드아웃).
 *
 * <p>카드별 코스트 합이 모드 핸드 장수(Compact 4 / Full 5)와 같아야 한다.</p>
 */
public record UpsertMyPitchCardsRequest(

        /** 보유 구종 마스터 카드 ID 목록 */
        @NotEmpty
        List<@NotNull Long> cardIds,

        /** 교체 시 제외할 카드 ID ({@code cardIds}에 포함) */
        @NotNull
        Long dropCardId
) {
}
