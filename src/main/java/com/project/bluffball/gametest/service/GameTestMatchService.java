package com.project.bluffball.gametest.service;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.GamePrepService;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.gametest.dto.TestMatchCreateResponse;
import com.project.bluffball.gametest.dto.TestMatchSetupNumbersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final MatchInfoRepository matchInfoRepository;
    private final GameStateRepository gameStateRepository;
    private final MatchInfoReader matchInfoReader;
    private final GamePrepService gamePrepService;

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
}
