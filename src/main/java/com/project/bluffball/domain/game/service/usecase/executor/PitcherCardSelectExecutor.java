package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.repository.TurnResultSessionRepository;
import com.project.bluffball.domain.game.service.usecase.reader.CoordinateCardReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameStateReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import com.project.bluffball.domain.game.enums.Timing;
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
    private final PitchCardReader pitchCardReader;

    /**
     * @return 투수가 선택한 시작 좌표 번호 (1~25) — 타자 공개용
     */
    public int execute(String matchSessionId,
                       Long pitchCardId,
                       Long coordinateCardId) {
        // 1. 투수가 제시한 카드 조합을 바탕으로 투구 데이터(시작/최종 위치, 구속 타이밍) 계산
        int startCoordinateNumber = coordinateCardReader.getPitcherStartCoordinateNumber(coordinateCardId);
        int finalCoordinateNumber = pitchCardReader.calculateFinalCoordinateNumber(pitchCardId, startCoordinateNumber);
        Timing pitchTiming = pitchCardReader.getPitchTiming(pitchCardId);

        // 2. 현재 게임의 진행 상황(이닝, 턴, 투수/타자 정보)과 투구 계산 결과를 묶어 턴 결과 세션 데이터 생성
        //    (이 정보들은 타자가 선택을 마칠 때까지 서버에 보안 상태로 유지됨)
        TurnResultSession session = TurnResultSession.builder()
                .matchSessionId(matchSessionId)
                .turnNumber(gameStateReader.getTurnNumber(matchSessionId))
                .inning(gameStateReader.getCurrentInning(matchSessionId))
                .isTop(gameStateReader.isTop(matchSessionId))
                .currentPitcherUserId(matchInfoReader.getPitcherUserId(matchSessionId))
                .currentBatterUserId(matchInfoReader.getCurrentBatterUserId(matchSessionId))
                .selectedPitchCardId(pitchCardId)
                .selectedCoordinateCardId(coordinateCardId)
                .startCoordinateNumber(startCoordinateNumber)
                .finalCoordinateNumber(finalCoordinateNumber)
                .pitchTiming(pitchTiming)
                .build();

        // 3. 타자의 선택 및 최종 턴 결과 판정을 위해 세션 저장소(Redis/DB)에 임시 저장
        turnResultSessionRepository.save(session);

        // 4. 타자에게 심리전용으로 선공개할 시작 좌표 번호만 반환
        return startCoordinateNumber;
    }
}
