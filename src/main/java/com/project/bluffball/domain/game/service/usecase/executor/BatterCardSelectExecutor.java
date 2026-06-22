package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.enums.TurnResult;
import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.domain.game.repository.TurnResultSessionRepository;
import com.project.bluffball.domain.game.service.usecase.judgment.BluffingJudgmentCalculator;
import com.project.bluffball.domain.game.service.usecase.judgment.TurnJudgmentCalculator;
import com.project.bluffball.domain.game.service.usecase.judgment.TurnJudgmentResult;
import com.project.bluffball.domain.game.service.usecase.reader.CoordinateCardReader;
import com.project.bluffball.domain.game.service.usecase.reader.GameStateReader;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.TurnResultSessionReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 타자 선택 저장 + 1·2차 타격 판정. 카운트·주자·점수는 {@link com.project.bluffball.domain.game.service.GameProgressService} 담당. */
@Component
@RequiredArgsConstructor
public class BatterCardSelectExecutor {

    private final TurnResultSessionRepository turnResultSessionRepository;
    private final TurnResultSessionReader turnResultSessionReader;
    private final GameStateRepository gameStateRepository;
    private final GameStateReader gameStateReader;
    private final MatchInfoReader matchInfoReader;
    private final CoordinateCardReader coordinateCardReader;
    private final TurnJudgmentCalculator turnJudgmentCalculator;
    private final BluffingJudgmentCalculator bluffingJudgmentCalculator;

    public TurnResult execute(String matchSessionId,
                              int batterCoordinateNumber,
                              Timing batterTiming,
                              double responseTimeSec) {
        // 투수 선택이 반영된 현재 턴 세션 (최종 좌표·투구 타이밍·투수/타자 userId)
        TurnResultSession session = turnResultSessionReader.getCurrentSession(matchSessionId);

        // 투수 구종 카드로 확정된 공의 착구 좌표 (PitcherCardSelectExecutor에서 세션에 저장됨)
        int finalCoordinateNumber = session.getFinalCoordinateNumber();

        // 시간초과(스윙 미발동) 시 S/B 판정, 폭투(좌표 0) 판정 등에 사용
        boolean finalCoordinateIsStrike = coordinateCardReader.isStrikeZone(finalCoordinateNumber);

        // [1차 판정] 시간초과 → S/B | 좌표 불일치 → STRIKE | 좌표·타이밍 일치 → 주사위(turnResult=null)
        TurnJudgmentResult preliminary = turnJudgmentCalculator.judge(
                finalCoordinateNumber,
                session.getPitchTiming(),
                batterCoordinateNumber,
                batterTiming,
                responseTimeSec,
                finalCoordinateIsStrike);

        // [2차 판정] 주사위가 있을 때만 블러핑 숫자 대조 → SINGLE~HR 등 타격 결과 확정
        TurnResult turnResult = resolveTurnResult(matchSessionId, session, preliminary);

        // 타자 선택·판정 결과를 TurnResultSession(Redis)에 기록
        session.applyBatterTurn(
                batterCoordinateNumber,
                preliminary.selectedTiming(),
                preliminary.diceResults(),
                turnResult);
        turnResultSessionRepository.save(session);

        // 턴 카운터만 +1 (볼·스트라이크·주자·점수는 GameProgressService에서 반영)
        GameState gameState = gameStateReader.getById(matchSessionId);
        gameState.advanceTurn();
        gameStateRepository.save(gameState);

        return turnResult;
    }

    /** 1차 판정 결과에서 최종 {@link TurnResult}만 추출·확정한다. */
    private TurnResult resolveTurnResult(String matchSessionId,
                                         TurnResultSession session,
                                         TurnJudgmentResult preliminary) {
        // S/B·폭투 등 1차에서 이미 확정 → 2차 생략
        // 주사위 목록이 비어 있으면(이론상 없음) 역시 2차 생략
        if (preliminary.turnResult() != null || preliminary.diceResults().isEmpty()) {
            return preliminary.turnResult();
        }

        // 주사위 눈금 합 ↔ setup-numbers로 등록한 블러핑 숫자 대조
        return bluffingJudgmentCalculator.judge(
                preliminary.diceResults(),
                matchInfoReader.getOutNumbers(matchSessionId, session.getCurrentPitcherUserId()),       // 투수 아웃 번호
                matchInfoReader.getDpNumbers(matchSessionId, session.getCurrentPitcherUserId()),        // 투수 병살 번호
                matchInfoReader.getTripleNumbers(matchSessionId, session.getCurrentBatterUserId()),       // 타자 3루타 번호
                matchInfoReader.getHrNumbers(matchSessionId, session.getCurrentBatterUserId()),         // 타자 홈런 번호
                gameStateReader.hasRunnersOnBase(matchSessionId));                                      // 병살: 주자 있을 때만 유효
    }
}

