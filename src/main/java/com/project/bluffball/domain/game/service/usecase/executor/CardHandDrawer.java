package com.project.bluffball.domain.game.service.usecase.executor;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 구종 카드 뽑기 전담 컴포넌트 (DB·Redis 접근 없음, 순수 계산).
 *
 * <h3>중복 방지 원칙</h3>
 * <ul>
 *   <li>{@link #draw} — 전체 풀에서 handSize만큼 중복 없이 뽑는다.</li>
 *   <li>{@link #redraw} — keepIds를 그대로 유지하고, 풀(pool = 전체 - keep)에서
 *       부족한 장수만큼 추가로 뽑는다. pool에는 keepIds가 제외되어 있으므로
 *       keepIds와 새로 뽑힌 카드 사이에 중복이 발생하지 않는다.</li>
 * </ul>
 */
@Component
public class CardHandDrawer {

    /**
     * 초기 카드 패 뽑기.
     *
     * @param allCardIds 전체 구종 카드 ID 풀
     * @param handSize   뽑을 장수(n)
     * @return 중복 없는 카드 ID 목록 (크기 = handSize)
     * @throws IllegalStateException 풀의 카드 수가 handSize보다 적을 때
     */
    public List<Long> draw(List<Long> allCardIds, int handSize) {
        if (allCardIds.size() < handSize) {
            throw new IllegalStateException(
                    "구종 카드 수(" + allCardIds.size() + ")가 핸드 장수(" + handSize + ")보다 적습니다.");
        }
        List<Long> shuffled = new ArrayList<>(allCardIds);
        Collections.shuffle(shuffled);
        return new ArrayList<>(shuffled.subList(0, handSize));
    }

    /**
     * 멀리건(카드 교체) 후 재뽑기.
     *
     * <p>keepIds를 그대로 유지하고, 전체 풀에서 keepIds를 제외한 카드들 중
     * 부족한 장수(handSize - keepIds.size())만큼 추가로 뽑는다.</p>
     *
     * @param allCardIds 전체 구종 카드 ID 풀
     * @param keepIds    유지할 카드 ID 목록 (교체하지 않는 카드)
     * @param handSize   최종 핸드 장수(n)
     * @return 유지 카드 + 새로 뽑은 카드를 합친 목록 (크기 = handSize)
     * @throws IllegalStateException 재뽑기 풀의 카드 수가 부족할 때
     */
    public List<Long> redraw(List<Long> allCardIds, List<Long> keepIds, int handSize) {
        int drawCount = handSize - keepIds.size();

        Set<Long> keepSet = new HashSet<>(keepIds);
        List<Long> pool = new ArrayList<>();
        for (Long id : allCardIds) {
            if (!keepSet.contains(id)) {
                pool.add(id);
            }
        }

        if (pool.size() < drawCount) {
            throw new IllegalStateException(
                    "재뽑기 풀의 카드 수(" + pool.size() + ")가 부족합니다. 필요=" + drawCount);
        }

        Collections.shuffle(pool);
        List<Long> result = new ArrayList<>(keepIds);
        result.addAll(pool.subList(0, drawCount));
        return result;
    }
}
