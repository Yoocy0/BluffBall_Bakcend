package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.repository.TurnResultSessionRepository;
import com.project.bluffball.domain.game.service.usecase.reader.CoordinateCardReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameStateReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 투수 카드 선택 저장 전담 Executor.
 *
 * <p>선택값과 함께 구종 변화 적용 후 최종 좌표·구종 타이밍을 확정하여 TurnResultSession에 저장한다.
 * 클라이언트에는 시작 좌표만 공개하며, 저장된 최종 좌표·타이밍은 타자 선택 후 판정에 사용한다.</p>
 */
@Component
@RequiredArgsConstructor
public class PitcherCardSelectExecutor {

    private final TurnResultSessionRepository turnResultSessionRepository;
    private final MatchInfoReader matchInfoReader;
    private final GameStateReader gameStateReader;
    private final CoordinateCardReader coordinateCardReader;
    private final UserPitchCardReader userPitchCardReader;

    /**
     * @return 투수가 선택한 시작 좌표 번호 (1~25) — 타자 공개용
     */
    public int execute(String matchSessionId,
                       Long pitchCardId,
                       Long coordinateCardId) {
        Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);

        // 1. 투수 카드 조합 → 시작/최종 좌표·타이밍 (유저 강화 오버레이 반영)
        int startCoordinateNumber = coordinateCardReader.getPitcherStartCoordinateNumber(coordinateCardId);
        int finalCoordinateNumber = userPitchCardReader.calculateFinalCoordinateNumber(
                pitcherUserId, pitchCardId, startCoordinateNumber);
        Timing pitchTiming = userPitchCardReader.getEffectiveTiming(pitcherUserId, pitchCardId);

        // 2. 턴 결과 세션에 투구 확정값 저장 (타자 선택 전까지 비공개)
        TurnResultSession session = TurnResultSession.builder()
                .matchSessionId(matchSessionId)
                .turnNumber(gameStateReader.getTurnNumber(matchSessionId))
                .inning(gameStateReader.getCurrentInning(matchSessionId))
                .isTop(gameStateReader.isTop(matchSessionId))
                .currentPitcherUserId(pitcherUserId)
                .currentBatterUserId(matchInfoReader.getCurrentBatterUserId(matchSessionId))
                .selectedPitchCardId(pitchCardId)
                .selectedCoordinateCardId(coordinateCardId)
                .startCoordinateNumber(startCoordinateNumber)
                .finalCoordinateNumber(finalCoordinateNumber)
                .pitchTiming(pitchTiming)
                .build();

        turnResultSessionRepository.save(session);
        return startCoordinateNumber;
    }
}
