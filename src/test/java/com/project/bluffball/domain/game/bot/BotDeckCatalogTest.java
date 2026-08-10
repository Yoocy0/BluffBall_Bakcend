package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.card.enums.EnhancementEffect;
import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.card.repository.PitchCardRepository;
import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.tutorial.TutorialConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BotDeckCatalog")
class BotDeckCatalogTest {

    @Mock
    private PitchCardRepository pitchCardRepository;

    private BotDeckCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new BotDeckCatalog(pitchCardRepository);
    }

    @Test
    @DisplayName("기본 덱 이름은 튜토리얼 고정+선택 4장")
    void basicDeckNamesFromTutorialConstants() {
        assertThat(catalog.basicDeckNames()).containsExactly(
                TutorialConstants.FIXED_STARTER_PITCH_NAME,
                "커브",
                "슬라이더",
                "포크");
        assertThat(BotBasicDeck.PITCH_NAMES).hasSize(4);
        assertThat(BotBasicDeck.PITCH_NAMES).containsAll(TutorialConstants.SELECTABLE_STARTER_PITCH_NAMES);
        assertThat(BotBasicDeck.PITCH_NAMES).contains(TutorialConstants.FIXED_STARTER_PITCH_NAME);
    }

    @Test
    @DisplayName("EASY 마스터 풀은 기본 덱 4장")
    void easyMastersAreBasicFour() {
        PitchCard fourSeem = master(1L, "포심 패스트볼", Timing.EARLY);
        PitchCard curve = master(2L, "커브", Timing.LATE);
        PitchCard slider = master(3L, "슬라이더", Timing.NORMAL);
        PitchCard fork = master(4L, "포크", Timing.NORMAL);
        when(pitchCardRepository.findByNameIn(anyCollection()))
                .thenReturn(List.of(fourSeem, curve, slider, fork));

        assertThat(catalog.masterPitchCardIdsFor(BotDifficulty.EASY))
                .containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    @DisplayName("NORMAL 마스터 풀은 전체 마스터 크기")
    void normalMastersAreAll() {
        when(pitchCardRepository.findAllIds()).thenReturn(List.of(1L, 2L, 3L, 4L, 5L, 6L));

        assertThat(catalog.masterPitchCardIdsFor(BotDifficulty.NORMAL))
                .hasSize(6)
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    }

    @Test
    @DisplayName("HARD 마스터 목록은 NORMAL과 같고, 예상 풀은 마스터보다 큼(최대 약 4배)")
    void hardPoolLargerThanMasters() {
        when(pitchCardRepository.findAllIds()).thenReturn(List.of(10L, 20L, 30L));
        List<Timing> timings = List.of(Timing.EARLY, Timing.NORMAL, Timing.LATE);

        assertThat(catalog.masterPitchCardIdsFor(BotDifficulty.HARD)).hasSize(3);

        int hardSize = BotDeckCatalog.hardPoolSizeFor(timings);
        assertThat(hardSize).isGreaterThan(timings.size());
        assertThat(hardSize).isLessThanOrEqualTo(timings.size() * 4);
        // 현재 시드 타이밍에는 TOO_EARLY/TOO_LATE가 없어 마스터당 기본+3강화 = 4
        assertThat(hardSize).isEqualTo(timings.size() * 4);
    }

    @Test
    @DisplayName("TOO_EARLY는 FASTER 스킵, TOO_LATE는 SLOWER 스킵")
    void timingBoundarySkips() {
        assertThat(BotDeckCatalog.isTimingEnhancementLegal(Timing.TOO_EARLY, TimingEnhancement.FASTER))
                .isFalse();
        assertThat(BotDeckCatalog.isTimingEnhancementLegal(Timing.TOO_EARLY, TimingEnhancement.SLOWER))
                .isTrue();
        assertThat(BotDeckCatalog.isTimingEnhancementLegal(Timing.TOO_LATE, TimingEnhancement.SLOWER))
                .isFalse();
        assertThat(BotDeckCatalog.isTimingEnhancementLegal(Timing.TOO_LATE, TimingEnhancement.FASTER))
                .isTrue();

        assertThat(BotDeckCatalog.legalSingleEnhancements(Timing.TOO_EARLY))
                .containsExactly(
                        EnhancementEffect.CHANGE_AMOUNT_PLUS_1,
                        EnhancementEffect.TIMING_SLOWER);
        assertThat(BotDeckCatalog.legalSingleEnhancements(Timing.TOO_LATE))
                .containsExactly(
                        EnhancementEffect.CHANGE_AMOUNT_PLUS_1,
                        EnhancementEffect.TIMING_FASTER);

        int size = BotDeckCatalog.hardPoolSizeFor(List.of(Timing.TOO_EARLY, Timing.TOO_LATE));
        // 각 마스터: 기본 + 변화량 + 타이밍 1종 = 3 → 합 6 (4×2=8보다 작음)
        assertThat(size).isEqualTo(6);
    }

    @Test
    @DisplayName("null 난이도는 빈 목록")
    void nullDifficulty() {
        assertThat(catalog.masterPitchCardIdsFor(null)).isEmpty();
    }

    /**
     * 테스트용 마스터 Entity를 만든다 (id 리플렉션 주입).
     *
     * @param id     마스터 ID
     * @param name   이름
     * @param timing 타이밍
     * @return PitchCard
     */
    private static PitchCard master(Long id, String name, Timing timing) {
        PitchCard card = new PitchCard(name, 1, ChangeDirection.DOWN, timing);
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }
}
