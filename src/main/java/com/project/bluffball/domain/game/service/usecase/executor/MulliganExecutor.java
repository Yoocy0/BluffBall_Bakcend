package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.card.service.usecase.reader.UserPitchCardReader;
import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 멀리건(카드 교체) 실행 및 Redis 저장 전담 컴포넌트.
 *
 * <p>사람 참가자는 보유 {@code UserPitchCard} 인스턴스 ID 풀에서 재드로우하고,
 * 연습 봇은 매치에 저장된 {@code botDrawPool}을 사용한다.
 * {@link CardHandDrawer#redraw}가 keepIds를 풀에서 제외하므로 인스턴스 ID 풀이면
 * 유지한 카드가 다시 뽑히지 않는다.</p>
 */
@Component
@RequiredArgsConstructor
public class MulliganExecutor {

    private final MatchInfoRepository matchInfoRepository;
    private final MatchInfoReader matchInfoReader;
    private final UserPitchCardReader userPitchCardReader;
    private final CardHandDrawer cardHandDrawer;
    private final GameModeRule gameModeRule;

    /**
     * 선택한 카드를 교체한 뒤 최종 핸드를 저장한다.
     *
     * <p>재드로우 풀이 부족하면 {@code GAME_CARD_REDRAW_INSUFFICIENT}가 발생한다.</p>
     *
     * @param matchSessionId  매치 세션 ID
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
     *
     * @param matchSessionId 매치 세션 ID
     * @param userId         확정하는 참가자 userId
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
     * <p>연습 봇이면 저장된 botDrawPool, 아니면 해당 유저 보유 인스턴스 ID 전체.</p>
     *
     * @param matchInfo 매치
     * @param userId    참가자
     * @return 풀 ID 목록 (인스턴스 ID)
     */
    private List<Long> resolveDrawPool(MatchInfo matchInfo, Long userId) {
        if (matchInfo.isPracticeBotUser(userId) && matchInfo.hasBotDrawPool()) {
            return matchInfo.getBotDrawPool();
        }
        return userPitchCardReader.findOwnedInstanceIds(userId);
    }
}
