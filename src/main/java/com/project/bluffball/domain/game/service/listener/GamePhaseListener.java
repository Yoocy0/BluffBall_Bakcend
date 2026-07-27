package com.project.bluffball.domain.game.service.listener;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.event.HalfInningChangedEvent;
import com.project.bluffball.domain.game.event.SetupNumbersSubmittedEvent;
import com.project.bluffball.domain.game.service.GamePrepService;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.user.record.enums.GameMode;
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
    private final GameModeRule gameModeRule;

    /**
     * 블러핑 숫자 제출 이벤트 수신.
     *
     * <p>쇼다운: 양측 완료 시 카드 드로우.
     * 리그: 역할별 셋업 완료 시 바로 경기 시작(드로우·멀리건 없음).</p>
     *
     * @param event 셋업 제출 이벤트
     */
    @EventListener
    public void onSetupNumbersSubmitted(SetupNumbersSubmittedEvent event) {
        String matchSessionId = event.matchSessionId();
        if (!matchInfoReader.isSetupNumbersComplete(matchSessionId)) {
            return;
        }
        GameMode gameMode = matchInfoReader.getGameMode(matchSessionId);
        if (gameModeRule.usesInGameCardDrawAndMulligan(gameMode)) {
            gamePrepService.drawCardHand(matchSessionId);
            return;
        }
        gamePrepService.beginLeaguePlayIfReady(matchSessionId);
    }

    /**
     * 공수 교대 이벤트 수신 — 역할 교환만 수행(멀리건·드로우 없음).
     *
     * @param event 공수 교대 이벤트
     */
    @EventListener
    public void onHalfInningChanged(HalfInningChangedEvent event) {
        gamePrepService.beginHalfInningRoleSwap(event.matchSessionId());
    }
}
