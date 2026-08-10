package com.project.bluffball.domain.game.dto.response;

import com.project.bluffball.domain.game.enums.BotDifficulty;

/**
 * Showdown 봇 매치 시작 응답.
 *
 * @param matchSessionId 매치 세션 ID — WebSocket {@code /ws} 연결·구독에 사용
 * @param botUserId      상대 봇 유저 ID
 * @param difficulty     적용된 난이도
 * @param fromMatchmaking 매칭 큐에서 전환했는지
 */
public record BotMatchStartResponse(
        String matchSessionId,
        Long botUserId,
        BotDifficulty difficulty,
        boolean fromMatchmaking
) {
}
