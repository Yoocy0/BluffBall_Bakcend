package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.game.dto.response.CardInfo;

/**
 * HARD 투수 봇의 실현 가능한 가중 옵션.
 *
 * @param kind                   옵션 종류
 * @param pitch                  선택한 구종
 * @param startCoordinateNumber  시작 좌표 (1~25)
 * @param weight                 샘플링 가중치
 */
record PitcherBotHardOption(
        PitcherBotOptionKind kind,
        CardInfo pitch,
        int startCoordinateNumber,
        int weight
) {
}
