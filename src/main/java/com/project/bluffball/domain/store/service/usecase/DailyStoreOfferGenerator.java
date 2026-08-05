package com.project.bluffball.domain.store.service.usecase;

import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.store.StoreConstants;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 유저·날짜 시드로 일일 구종 오퍼를 결정한다.
 */
@Component
public class DailyStoreOfferGenerator {

    /**
     * 오늘 노출할 구종 목록을 결정한다 (순서·구성은 userId+날짜로 결정적).
     *
     * @param userId     유저 ID
     * @param offerEpoch 오퍼 날짜 epoch day (KST)
     * @param catalog    마스터 구종 전체
     * @return 오퍼 구종 (최대 {@link StoreConstants#DAILY_OFFER_COUNT})
     */
    public List<PitchCard> generate(Long userId, long offerEpoch, List<PitchCard> catalog) {
        if (catalog == null || catalog.isEmpty()) {
            return List.of();
        }
        List<PitchCard> shuffled = new ArrayList<>(catalog);
        shuffled.sort(Comparator.comparing(PitchCard::getId));
        long seed = userId * 31_337L + offerEpoch;
        Collections.shuffle(shuffled, new Random(seed));
        int count = Math.min(StoreConstants.DAILY_OFFER_COUNT, shuffled.size());
        return List.copyOf(shuffled.subList(0, count));
    }
}
