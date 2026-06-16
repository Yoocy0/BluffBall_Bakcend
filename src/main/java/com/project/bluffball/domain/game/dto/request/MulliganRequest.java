package com.project.bluffball.domain.game.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 투수 카드 교체(멀리건) WebSocket 수신 DTO.
 *
 * <p>카드 드로우 및 블러핑 숫자 선택 완료 이후, 투수가 초기 패에서
 * 마음에 들지 않는 카드를 새 카드로 교체하거나 교체 없이 확정할 때 사용한다.
 * 멀리건은 경기 당 1회만 허용된다.</p>
 *
 * <ul>
 *   <li>교체할 카드가 있는 경우: {@code cardIdsToSwap}에 교체 대상 카드 ID 목록을 담아 전송한다.</li>
 *   <li>교체 없이 확정하는 경우: {@code cardIdsToSwap}에 빈 리스트를 담아 전송한다.</li>
 * </ul>
 *
 * <p>서버는 요청 처리 후 최종 확정된 카드 패를 {@code CardHandEvent}로 해당 투수에게 재전송한다.</p>
 */
@Getter
@NoArgsConstructor
public class MulliganRequest {

    /**
     * 교체할 구종 카드 ID 목록.
     * 빈 리스트 전송 시 현재 패를 그대로 확정한다.
     * 싱글 모드 최대 3장 / 팀전 최대 5장 (초기 패 전체 교체 가능).
     */
    @NotNull(message = "cardIdsToSwap은 null일 수 없습니다.")
    private List<Long> cardIdsToSwap;
}
