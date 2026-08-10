package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 멀리건(카드 교체) 실행 및 Redis 저장 전담 컴포넌트.
 */
@Component
@RequiredArgsConstructor
public class MulliganExecutor {

    private final MatchInfoRepository matchInfoRepository;
    private final MatchInfoReader matchInfoReader;
    private final PitchCardReader pitchCardReader;
    private final CardHandDrawer cardHandDrawer;
    private final GameModeRule gameModeRule;

    /**
     * @param userId          멀리건을 진행하는 참가자 userId
     * @param cardIdsToSwap   교체할 카드 ID 목록 (비어있지 않음을 Service에서 보장)
     * @return 교체 후 최종 카드 패 ID 목록
     */
    public List<Long> execute(String matchSessionId, Long userId, List<Long> cardIdsToSwap) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        List<Long> currentHand = new ArrayList<>(matchInfo.getPlayerCardHand(userId));

        int handSize = gameModeRule.getHandSize(matchInfo.getGameMode());

        Set<Long> swapSet = new HashSet<>(cardIdsToSwap);
        List<Long> keepIds = currentHand.stream()
                .filter(id -> !swapSet.contains(id))
                .collect(Collectors.toList());

        List<Long> allCardIds = resolveDrawPool(matchInfo, userId);
        List<Long> newHand = cardHandDrawer.redraw(allCardIds, keepIds, handSize);

        matchInfo.setPlayerCardHand(userId, newHand);
        matchInfo.completeMulliganForUser(userId);
        matchInfoRepository.save(matchInfo);

        return newHand;
    }

    /**
     * 교체 없이 확정하는 경우에 호출된다.
     */
    public void confirm(String matchSessionId, Long userId) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        matchInfo.completeMulliganForUser(userId);
        matchInfo.syncPitcherCardHandFromPlayer();
        matchInfoRepository.save(matchInfo);
    }

    /**
     * 참가자별 멀리건 재드로우 풀을 고른다.
     *
     * @param matchInfo 매치
     * @param userId    참가자
     * @return 풀 ID 목록
     */
    private List<Long> resolveDrawPool(MatchInfo matchInfo, Long userId) {
        if (matchInfo.isPracticeBotUser(userId) && matchInfo.hasBotDrawPool()) {
            return matchInfo.getBotDrawPool();
        }
        return pitchCardReader.findAllIds();
    }
}
