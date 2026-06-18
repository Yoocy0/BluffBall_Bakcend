package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 초기 카드 패 뽑기 실행 및 Redis 저장 전담 컴포넌트.
 *
 * <p>매치 생성 직후 서버가 호출한다.
 * 모드별 핸드 장수를 내부에서 결정하여 뽑기 후 MatchInfo.pitcherCardHand를 갱신한다.</p>
 */
@Component
@RequiredArgsConstructor
public class CardHandDrawExecutor {

    private final MatchInfoRepository matchInfoRepository;
    private final MatchInfoReader matchInfoReader;
    private final PitchCardReader pitchCardReader;
    private final CardHandDrawer cardHandDrawer;
    private final GameModeRule gameModeRule;

    /**
     * @param matchSessionId 매치 세션 ID
     * @return 뽑힌 카드 ID 목록
     */
    public List<Long> execute(String matchSessionId) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);

        int handSize = gameModeRule.getHandSize(matchInfo.getGameMode());
        List<Long> allIds = pitchCardReader.findAllIds();
        List<Long> drawnIds = cardHandDrawer.draw(allIds, handSize);

        matchInfo.getPitcherCardHand().clear();
        matchInfo.getPitcherCardHand().addAll(drawnIds);
        matchInfoRepository.save(matchInfo);

        return drawnIds;
    }
}
