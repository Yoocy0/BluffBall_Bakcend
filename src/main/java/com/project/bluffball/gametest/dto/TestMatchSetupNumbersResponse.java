package com.project.bluffball.gametest.dto;

import java.util.List;
import java.util.Map;

/**
 * 테스트용 — Redis에 저장된 블러핑 숫자 조회 응답.
 */
public record TestMatchSetupNumbersResponse(
        String matchSessionId,
        Map<Long, List<Integer>> outNumbers,
        Map<Long, List<Integer>> dpNumbers,
        Map<Long, List<Integer>> tripleNumbers,
        Map<Long, List<Integer>> hrNumbers,
        boolean setupComplete
) {
}
