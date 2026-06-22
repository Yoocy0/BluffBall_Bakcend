package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.game.service.usecase.reader.PitchCardReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 멀리건(카드 교체) 실행 및 Redis 저장 전담 컴포넌트.
 *
 * <p>모드별 핸드 장수를 내부에서 결정하므로 Service는 handSize를 알 필요가 없다.
 * 교체 목록의 카드를 패에서 제거하고, 전체 풀에서 keep 카드를 제외한 나머지 중
 * 부족한 장수만큼 재뽑기한 뒤 MatchInfo.pitcherCardHand를 갱신하여 Redis에 저장한다.</p>
 *
 * <p>교체할 카드가 없는 경우(빈 리스트) 이 Executor는 호출되지 않는다.
 * 호출 여부 판단은 Service 레이어에서 담당한다.</p>
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
     * @param matchSessionId 매치 세션 ID
     * @param cardIdsToSwap  교체할 카드 ID 목록 (비어있지 않음을 Service에서 보장)
     * @return 교체 후 최종 카드 패 ID 목록
     */
    public List<Long> execute(String matchSessionId, List<Long> cardIdsToSwap) {
        // 매치 정보 조회 및 존재 여부 확인
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        // 초기 카드 리스트(id)
        List<Long> currentHand = matchInfo.getPitcherCardHand();

        // 매치에 따른 카드 장수
        int handSize = gameModeRule.getHandSize(matchInfo.getGameMode());

        // 교체 요청된 카드 셋(id)
        Set<Long> swapSet = new HashSet<>(cardIdsToSwap);
        // 교체 요청되지 않은 카드 리스트(id)
        List<Long> keepIds = currentHand.stream()
                .filter(id -> !swapSet.contains(id))
                .collect(Collectors.toList());

        // 전체 카드 리스트(id)
        List<Long> allCardIds = pitchCardReader.findAllIds();
        // 새롭게 뽑을 카드 리스트(id)
        List<Long> newHand = cardHandDrawer.redraw(allCardIds, keepIds, handSize);

        // 현재 들고있는 카드 초기화
        currentHand.clear();
        // 최종 결정된 카드 추가
        currentHand.addAll(newHand);
        // 매치에 멀리건 작업 완료 전달
        matchInfo.completeMulligan();
        matchInfoRepository.save(matchInfo);

        return newHand;
    }

    /**
     * 교체 없이 확정하는 경우에 호출된다.
     * 멀리건 완료 플래그만 저장하고 카드 패는 그대로 유지한다.
     */
    public void confirm(String matchSessionId) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        matchInfo.completeMulligan();
        matchInfoRepository.save(matchInfo);
    }
}
