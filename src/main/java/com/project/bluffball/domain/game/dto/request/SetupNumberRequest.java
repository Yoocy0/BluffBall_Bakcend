package com.project.bluffball.domain.game.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 블러핑 고유 숫자 제출 WebSocket 수신 DTO.
 *
 * <p>인게임 시작 직후 각 플레이어가 1~12 중에서 예측 숫자를 제출한다.
 * 주사위 눈금 합이 이 숫자들과 일치하면 특수 판정이 발동된다.</p>
 *
 * <p>모든 필드를 List로 받는 이유:
 * <ul>
 *   <li>투수 교체 등판 시 아웃/병살 숫자 개수가 줄어들 수 있다.</li>
 *   <li>클랜전 스킬로 숫자를 1개 더 추가하는 확장을 수용하기 위해서다.</li>
 * </ul>
 * 기본 싱글 모드 기준: outNumList 5개 / dpNumList 1개 / tripleNumList 1개 / hrNumList 1개
 * </p>
 */
public record SetupNumberRequest(
        /** 투수의 아웃 유발 번호 목록 */
        @NotNull
        List<@Min(1) @Max(12) Integer> outNumList,
        /** 투수의 병살 유발 번호 목록 */
        @NotNull
        List<@Min(1) @Max(12) Integer> dpNumList,
        /** 타자의 3루타 유발 번호 목록 */
        @NotNull
        List<@Min(1) @Max(12) Integer> tripleNumList,
        /** 타자의 홈런 유발 번호 목록 */
        @NotNull
        List<@Min(1) @Max(12) Integer> hrNumList
) {
}
