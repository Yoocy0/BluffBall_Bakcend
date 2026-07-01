package com.project.bluffball.domain.game.service.listener;

import com.project.bluffball.domain.game.event.HalfInningChangedEvent;
import com.project.bluffball.domain.game.event.SetupNumbersSubmittedEvent;
import com.project.bluffball.domain.game.service.GamePrepService;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 인게임 페이즈 전환 리스너.
 *
 * <p>각 페이즈 완료 이벤트를 수신하여 다음 페이즈를 시작한다.
 * 페이즈 간 전환 조건 판단은 여기서만 처리하며,
 * 각 Service 메서드는 자신의 역할만 수행하고 다음 단계를 알지 못한다.</p>
 */
@Component
@RequiredArgsConstructor
public class GamePhaseListener {

    private final MatchInfoReader matchInfoReader;
    private final GamePrepService gamePrepService;

    /**
     * 블러핑 숫자 제출 이벤트 수신.
     *
     * <p>양측 제출이 완료된 경우에만 초기 카드 드로우를 시작한다.</p>
     */
    @EventListener
    public void onSetupNumbersSubmitted(SetupNumbersSubmittedEvent event) {
        if (matchInfoReader.isSetupNumbersComplete(event.matchSessionId())) {
            gamePrepService.drawCardHand(event.matchSessionId());
        }
    }

    /**
     * 공수 교대 이벤트 수신 — 역할 교환만 수행(멀리건·드로우 없음).
     */
    @EventListener
    public void onHalfInningChanged(HalfInningChangedEvent event) {
        gamePrepService.beginHalfInningRoleSwap(event.matchSessionId());
    }
}
