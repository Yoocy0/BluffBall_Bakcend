package com.project.bluffball.domain.game.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * 블러핑 고유 숫자 제출 WebSocket 수신 DTO.
 *
 * <p>인게임 시작 직후 각 플레이어가 1~12 중에서 예측 숫자를 제출한다.
 * 주사위 눈금 합이 이 숫자들과 일치하면 특수 판정이 발동된다.</p>
 *
 * <p>모드별 제출:
 * <ul>
 *   <li>쇼다운({@code FULL}) — out 5 / dp 1 / triple 1 / hr 1 일괄</li>
 *   <li>리그 투수({@code PITCHER}) — out·dp만 (triple/hr 생략 가능)</li>
 *   <li>리그 타자({@code BATTER}) — triple·hr만 (out/dp 생략 가능)</li>
 * </ul>
 * 리그는 매치 초 전원 타자 셋업 + 양 선발/현재 투수 셋업.
 * 투수 교체 시에만 신임 투수/강판 투수가 역할별 셋업을 재제출한다.</p>
 */
public record SetupNumberRequest(
        /** 투수의 아웃 유발 번호 목록 */
        List<@Min(1) @Max(12) Integer> outNumList,
        /** 투수의 병살 유발 번호 목록 */
        List<@Min(1) @Max(12) Integer> dpNumList,
        /** 타자의 3루타 유발 번호 목록 */
        List<@Min(1) @Max(12) Integer> tripleNumList,
        /** 타자의 홈런 유발 번호 목록 */
        List<@Min(1) @Max(12) Integer> hrNumList
) {
}
