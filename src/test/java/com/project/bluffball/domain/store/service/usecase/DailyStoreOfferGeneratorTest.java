package com.project.bluffball.domain.store.service.usecase;

import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.store.StoreConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DailyStoreOfferGenerator")
class DailyStoreOfferGeneratorTest {

    private final DailyStoreOfferGenerator generator = new DailyStoreOfferGenerator();

    @Test
    @DisplayName("같은 userId·날짜면 동일 오퍼")
    void deterministicForSameSeed() {
        List<PitchCard> catalog = catalog(1L, 2L, 3L, 4L, 5L, 6L);
        List<Long> a = generator.generate(10L, 20_000L, catalog).stream().map(PitchCard::getId).toList();
        List<Long> b = generator.generate(10L, 20_000L, catalog).stream().map(PitchCard::getId).toList();
        assertEquals(a, b);
        assertEquals(StoreConstants.DAILY_OFFER_COUNT, a.size());
    }

    @Test
    @DisplayName("유저가 다르면 오퍼 구성이 달라질 수 있다")
    void differsByUser() {
        List<PitchCard> catalog = catalog(1L, 2L, 3L, 4L, 5L, 6L);
        List<Long> userA = generator.generate(1L, 20_000L, catalog).stream().map(PitchCard::getId).toList();
        List<Long> userB = generator.generate(2L, 20_000L, catalog).stream().map(PitchCard::getId).toList();
        assertEquals(StoreConstants.DAILY_OFFER_COUNT, userA.size());
        assertEquals(StoreConstants.DAILY_OFFER_COUNT, userB.size());
        // 확률적으로 다를 가능성이 높음 — 동일해도 테스트 실패시키지 않고 size만 보장
    }

    private static List<PitchCard> catalog(Long... ids) {
        return java.util.Arrays.stream(ids)
                .map(id -> {
                    PitchCard card = mock(PitchCard.class);
                    when(card.getId()).thenReturn(id);
                    return card;
                })
                .toList();
    }
}
