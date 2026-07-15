package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 구종 카드 뽑기 전담 컴포넌트 (DB·Redis 접근 없음, 순수 계산).
 */
@Component
public class CardHandDrawer {

    public List<Long> draw(List<Long> allCardIds, int handSize) {
        if (allCardIds.size() < handSize) {
            throw new BadRequestException(
                    ErrorCode.GAME_CARD_POOL_INSUFFICIENT,
                    "poolSize=" + allCardIds.size() + ", handSize=" + handSize);
        }
        List<Long> shuffled = new ArrayList<>(allCardIds);
        Collections.shuffle(shuffled);
        return new ArrayList<>(shuffled.subList(0, handSize));
    }

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
            throw new BadRequestException(
                    ErrorCode.GAME_CARD_REDRAW_INSUFFICIENT,
                    "poolSize=" + pool.size() + ", required=" + drawCount);
        }

        Collections.shuffle(pool);
        List<Long> result = new ArrayList<>(keepIds);
        result.addAll(pool.subList(0, drawCount));
        return result;
    }
}
