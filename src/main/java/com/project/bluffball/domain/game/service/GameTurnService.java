package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.dto.request.BatterCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.PitcherCardSelectRequest;
import com.project.bluffball.domain.game.dto.response.PitcherReadyEvent;
import com.project.bluffball.domain.game.service.usecase.executor.PitcherCardSelectExecutor;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.validator.PitcherCardSelectValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 인게임 반복 턴 실행 서비스.
 *
 * <p>투수/타자 카드 선택, 타격 판정, 결과 브로드캐스트를 담당한다.
 * 한 게임에서 수십~수백 번 반복 호출된다.</p>
 *
 * <p>Entity·Repository에 직접 접근하지 않는다.
 * 모든 검증은 Validator, 상태 변경은 Executor, 조회는 Reader에 위임한다.</p>
 *
 * @see GamePrepService 준비 단계(숫자 셋업, 카드 드로우·멀리건)
 */
@Service
@RequiredArgsConstructor
public class GameTurnService {

    private final PitcherCardSelectValidator pitcherCardSelectValidator;
    private final PitcherCardSelectExecutor pitcherCardSelectExecutor;
    private final MatchInfoReader matchInfoReader;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String GAME_TOPIC = "/topic/game/";

    /**
     * 투수 구종 카드 및 시작 좌표 카드 선택을 처리한다.
     *
     * <p>처리 완료 후 타자에게 시작 좌표 번호만 공개하는 {@code PitcherReadyEvent}를 전송한다.
     * 최종 좌표와 구종 타이밍은 타자 선택 후 {@code TurnResultEvent}에서 공개한다.</p>
     */
    public void pitcherSelectCard(String matchSessionId, Long userId, PitcherCardSelectRequest request) {
        List<Long> hand = matchInfoReader.getPitcherCardHand(matchSessionId);
        Long pitcherUserId = matchInfoReader.getPitcherUserId(matchSessionId);

        pitcherCardSelectValidator.validate(
                hand,
                pitcherUserId,
                userId,
                request.getPitchCardId(),
                request.getCoordinateCardId(),
                matchInfoReader.isMulliganDone(matchSessionId));

        int startCoordinateNumber = pitcherCardSelectExecutor.execute(
                matchSessionId,
                request.getPitchCardId(),
                request.getCoordinateCardId());

        PitcherReadyEvent event = PitcherReadyEvent.builder()
                .startCoordinateNumber(startCoordinateNumber)
                .build();
        messagingTemplate.convertAndSend(GAME_TOPIC + matchSessionId, event);
    }

    /**
     * 타자 예측 좌표 및 타이밍 선택을 처리하고 타격 이벤트를 판정한다.
     *
     * <p>판정 완료 후 투수와 타자 양측에 {@code TurnResultEvent}를 브로드캐스트한다.</p>
     */
    public void batterSelectCard(String matchSessionId, Long userId, BatterCardSelectRequest request) {
        // TODO
    }
}