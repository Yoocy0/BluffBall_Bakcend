package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.user.record.enums.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 싱글 모드 매치 생성 Executor.
 *
 * <p>MatchInfo·GameState를 Redis에 seed하고 matchSessionId만 반환한다.
 * Entity는 Service에 노출하지 않는다.</p>
 */
@Component
@RequiredArgsConstructor
public class SingleMatchCreateExecutor {

    /** 경기 설정·플레이어 정보 Redis Hash */
    private final MatchInfoRepository matchInfoRepository;

    /** 인게임 실시간 상태 Redis Hash */
    private final GameStateRepository gameStateRepository;

    /**
     * 싱글 모드(1 vs 1) 매치를 생성한다.
     *
     * @param pitcherUserId 선매칭(큐 선입) 유저 — 초기 투수
     * @param batterUserId  후매칭(큐 진입) 유저 — 초기 타자
     * @return 생성된 matchSessionId (UUID)
     */
    public String execute(Long pitcherUserId, Long batterUserId) {
        String matchSessionId = UUID.randomUUID().toString();

        MatchInfo matchInfo = MatchInfo.builder()
                .id(matchSessionId)
                .gameMode(GameMode.GENERAL)
                .pitcherUserId(pitcherUserId)
                .batterLineup(List.of(batterUserId))
                .build();
        matchInfo.ensureCollectionsInitialized();

        GameState gameState = GameState.builder()
                .id(matchSessionId)
                .build();

        matchInfoRepository.save(matchInfo);
        gameStateRepository.save(gameState);

        return matchSessionId;
    }
}
