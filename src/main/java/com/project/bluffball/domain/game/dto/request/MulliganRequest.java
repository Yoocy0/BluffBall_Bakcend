package com.project.bluffball.domain.game.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 투수 카드 교체(멀리건) WebSocket 수신 DTO.
 */
public record MulliganRequest(
        /**
         * 교체할 구종 카드 ID 목록.
         * 빈 리스트 전송 시 현재 패를 그대로 확정한다.
         */
        @NotNull(message = "cardIdsToSwap은 null일 수 없습니다.")
        List<Long> cardIdsToSwap
) {
}
