package com.project.bluffball.gametest.dto;

/**
 * 봇 자동 플레이 시작 응답.
 *
 * @param matchSessionId 매치 세션
 * @param botCount 등록된 봇 수
 * @param running 루프 기동 여부
 */
public record StartBotAutoPlayResponse(
        String matchSessionId,
        int botCount,
        boolean running
) {
}
