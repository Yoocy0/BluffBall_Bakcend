package com.project.bluffball.gametest.service;

import com.project.bluffball.domain.card.entity.CoordinateCard;
import com.project.bluffball.domain.card.repository.CoordinateCardRepository;
import com.project.bluffball.domain.game.dto.request.MulliganRequest;
import com.project.bluffball.domain.game.dto.request.PitcherCardSelectRequest;
import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.GamePrepService;
import com.project.bluffball.domain.game.service.GameTurnService;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import com.project.bluffball.domain.game.service.usecase.reader.TurnResultSessionReader;
import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.gametest.dto.TestCoordinateOption;
import com.project.bluffball.gametest.dto.TestMatchCreateResponse;
import com.project.bluffball.gametest.dto.TestMatchSetupNumbersResponse;
import com.project.bluffball.gametest.dto.TestPitchHandResponse;
import com.project.bluffball.gametest.dto.TestPitcherSelectResponse;
import com.project.bluffball.gametest.dto.TestPreparePitchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 게임 로직 수동 테스트용 임시 매치 생성.
 *
 * <p>본番 {@code MatchService} 구현 전까지 Redis에 MatchInfo·GameState를 seed한다.</p>
 */
@Service
@RequiredArgsConstructor
public class GameTestMatchService {

    private static final Long TEST_USER_ID = 1L;
    /** setup-numbers 완료 조건(2명) 충족용 상대 플레이어 */
    private static final Long TEST_OPPONENT_USER_ID = 2L;

    private final MatchInfoRepository matchInfoRepository;
    private final GameStateRepository gameStateRepository;
    private final MatchInfoReader matchInfoReader;
    private final PitchCardReader pitchCardReader;
    private final CoordinateCardRepository coordinateCardRepository;
    private final GamePrepService gamePrepService;
    private final GameTurnService gameTurnService;
    private final TurnResultSessionReader turnResultSessionReader;

    @Transactional
    public TestMatchCreateResponse createSingleTestMatch() {
        String matchSessionId = UUID.randomUUID().toString();

        MatchInfo matchInfo = MatchInfo.builder()
                .id(matchSessionId)
                .gameMode(GameMode.GENERAL)
                .pitcherUserId(TEST_USER_ID)
                .batterLineup(List.of(TEST_USER_ID))
                .build();
        matchInfo.ensureCollectionsInitialized();

        GameState gameState = GameState.builder()
                .id(matchSessionId)
                .build();

        matchInfoRepository.save(matchInfo);
        gameStateRepository.save(gameState);

        return new TestMatchCreateResponse(
                matchSessionId,
                TEST_USER_ID,
                TEST_USER_ID);
    }

    public TestMatchSetupNumbersResponse submitSetupNumbers(String matchSessionId, SetupNumberRequest request) {
        gamePrepService.setupNumbers(matchSessionId, TEST_USER_ID, request);
        return getSetupNumbers(matchSessionId);
    }

    public TestMatchSetupNumbersResponse getSetupNumbers(String matchSessionId) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        return new TestMatchSetupNumbersResponse(
                matchSessionId,
                matchInfo.getOutNumbers(),
                matchInfo.getDpNumbers(),
                matchInfo.getTripleNumbers(),
                matchInfo.getHrNumbers(),
                matchInfoReader.isSetupNumbersComplete(matchSessionId));
    }

    /** 멀리건 화면 진입 — 2인 setup 완료 + 구종 3장 드로우 (멀리건은 하지 않음) */
    public TestPitchHandResponse prepareForDraw(String matchSessionId) {
        ensureSecondPlayerSetup(matchSessionId);

        if (matchInfoReader.getPitcherCardHand(matchSessionId).isEmpty()) {
            gamePrepService.drawCardHand(matchSessionId);
        }

        return getPitchHand(matchSessionId);
    }

    public TestPitchHandResponse getPitchHand(String matchSessionId) {
        List<Long> handIds = matchInfoReader.getPitcherCardHand(matchSessionId);
        return new TestPitchHandResponse(
                matchSessionId,
                matchInfoReader.isSetupNumbersComplete(matchSessionId),
                matchInfoReader.isMulliganDone(matchSessionId),
                pitchCardReader.getPitchCardDetails(handIds));
    }

    /** 멀리건 교체 — {@code cardIdsToSwap}에 선택한 카드 ID 전달 */
    public TestPitchHandResponse swapMulligan(String matchSessionId, MulliganRequest request) {
        gamePrepService.processMulligan(matchSessionId, TEST_USER_ID, request);
        return getPitchHand(matchSessionId);
    }

    /** 멀리건 확정 — 교체 없이 패 유지 */
    public TestPitchHandResponse confirmMulligan(String matchSessionId) {
        gamePrepService.processMulligan(matchSessionId, TEST_USER_ID, new MulliganRequest(List.of()));
        return getPitchHand(matchSessionId);
    }

    /** 투수 선택 화면 — 멀리건 완료 후 패·좌표 옵션 반환 */
    public TestPreparePitchResponse prepareForPitcher(String matchSessionId) {
        if (!matchInfoReader.isMulliganDone(matchSessionId)) {
            throw new IllegalStateException("멀리건(카드 교체/확정)을 먼저 완료하세요.");
        }

        TestPitchHandResponse hand = getPitchHand(matchSessionId);

        return new TestPreparePitchResponse(
                matchSessionId,
                hand.setupComplete(),
                hand.mulliganDone(),
                hand.pitchHand(),
                getPitcherCoordinateOptions());
    }

    public TestPitcherSelectResponse selectPitcherCard(String matchSessionId, PitcherCardSelectRequest request) {
        gameTurnService.pitcherSelectCard(matchSessionId, TEST_USER_ID, request);

        var session = turnResultSessionReader.getCurrentSession(matchSessionId);

        String pitchName = pitchCardReader.getPitchCardDetails(List.of(request.pitchCardId())).stream()
                .findFirst()
                .map(CardInfo::name)
                .orElse("?");

        return new TestPitcherSelectResponse(
                matchSessionId,
                session.getStartCoordinateNumber(),
                session.getFinalCoordinateNumber(),
                request.pitchCardId(),
                request.coordinateCardId(),
                pitchName);
    }

    private void ensureSecondPlayerSetup(String matchSessionId) {
        if (matchInfoReader.isSetupNumbersComplete(matchSessionId)) {
            return;
        }

        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        MatchInfo.PlayerSetupNumbers first = matchInfo.getPlayerSetupNumbers().stream()
                .filter(entry -> TEST_USER_ID.equals(entry.getUserId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "userId=" + TEST_USER_ID + "의 setup-numbers가 없습니다. 먼저 숫자를 제출하세요."));

        SetupNumberRequest duplicate = new SetupNumberRequest(
                first.getOutNumList(),
                first.getDpNumList(),
                first.getTripleNumList(),
                first.getHrNumList());
        gamePrepService.setupNumbers(matchSessionId, TEST_OPPONENT_USER_ID, duplicate);
    }

    private List<TestCoordinateOption> getPitcherCoordinateOptions() {
        return coordinateCardRepository.findAll().stream()
                .filter(card -> card.getCoordinateNumber() >= 1 && card.getCoordinateNumber() <= 25)
                .sorted(Comparator.comparingInt(CoordinateCard::getCoordinateNumber))
                .map(card -> new TestCoordinateOption(
                        card.getId(),
                        card.getCoordinateNumber(),
                        card.getName(),
                        card.isStrike()))
                .toList();
    }
}
