package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.enums.EnhancementEffect;
import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.card.repository.PitchCardRepository;
import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 난이도별 봇 구종 마스터 풀·HARD 단일 강화 변형 규칙 카탈로그.
 *
 * <p>인스턴스 시딩·드로우 풀 ID 확정은 {@code BotDeckInventoryExecutor}가 담당한다.</p>
 */
@Component
@RequiredArgsConstructor
public class BotDeckCatalog {

    /** 마스터 구종 Repository */
    private final PitchCardRepository pitchCardRepository;

    /**
     * 난이도에 해당하는 마스터 구종 카드 ID 목록을 반환한다.
     *
     * <p>EASY는 기본 덱 4장, NORMAL·HARD는 전체 마스터.
     * HARD의 강화 변형은 마스터 ID가 아니라 인스턴스 시딩 단계에서 확장된다.</p>
     *
     * @param difficulty 봇 난이도
     * @return 마스터 구종 ID 목록 (없으면 빈 목록)
     */
    public List<Long> masterPitchCardIdsFor(BotDifficulty difficulty) {
        if (difficulty == null) {
            return List.of();
        }
        return switch (difficulty) {
            case EASY -> resolveBasicMasterIds();
            case NORMAL, HARD -> List.copyOf(pitchCardRepository.findAllIds());
        };
    }

    /**
     * 기본 덱 구종 이름을 반환한다 ({@link BotBasicDeck}).
     *
     * @return 기본 덱 이름 목록
     */
    public List<String> basicDeckNames() {
        return BotBasicDeck.PITCH_NAMES;
    }

    /**
     * 전체 마스터 구종 Entity 목록을 반환한다 (Executor·카탈로그 내부용).
     *
     * @return 마스터 목록
     */
    public List<PitchCard> findAllMasters() {
        return pitchCardRepository.findAll();
    }

    /**
     * 마스터 기본 타이밍에 대해 허용되는 단일 강화 효과 목록을 반환한다.
     *
     * <p>{@link EnhancementEffect#CHANGE_AMOUNT_PLUS_1}은 항상 포함.
     * {@code TIMING_FASTER}/{@code TIMING_SLOWER}는
     * {@link com.project.bluffball.domain.card.service.usecase.validator.UserPitchCardValidator#validateTimingBoundary}
     * 와 동일 경계(가장 빠른 타이밍에 FASTER / 가장 느린 타이밍에 SLOWER 금지)를 적용한다.</p>
     *
     * @param baseTiming 마스터 기본 타이밍
     * @return 허용 효과 목록 (null이면 빈 목록)
     */
    public static List<EnhancementEffect> legalSingleEnhancements(Timing baseTiming) {
        if (baseTiming == null) {
            return List.of();
        }
        List<EnhancementEffect> effects = new ArrayList<>(3);
        effects.add(EnhancementEffect.CHANGE_AMOUNT_PLUS_1);
        if (isTimingEnhancementLegal(baseTiming, TimingEnhancement.FASTER)) {
            effects.add(EnhancementEffect.TIMING_FASTER);
        }
        if (isTimingEnhancementLegal(baseTiming, TimingEnhancement.SLOWER)) {
            effects.add(EnhancementEffect.TIMING_SLOWER);
        }
        return List.copyOf(effects);
    }

    /**
     * HARD 드로우 풀 예상 크기(마스터당 기본본 + 합법 단일 강화)를 계산한다.
     *
     * @param baseTimings 마스터 기본 타이밍 목록
     * @return 예상 풀 크기
     */
    public static int hardPoolSizeFor(Iterable<Timing> baseTimings) {
        if (baseTimings == null) {
            return 0;
        }
        int size = 0;
        for (Timing timing : baseTimings) {
            size += 1 + legalSingleEnhancements(timing).size();
        }
        return size;
    }

    /**
     * 타이밍 강화가 경계 밖으로 나가지 않는지 판정한다.
     *
     * @param baseTiming  마스터 타이밍
     * @param enhancement FASTER 또는 SLOWER
     * @return 합법이면 true
     */
    public static boolean isTimingEnhancementLegal(Timing baseTiming, TimingEnhancement enhancement) {
        if (baseTiming == null || enhancement == null || enhancement == TimingEnhancement.NONE) {
            return false;
        }
        int ordinal = baseTiming.ordinal();
        if (enhancement == TimingEnhancement.FASTER && ordinal <= Timing.TOO_EARLY.ordinal()) {
            return false;
        }
        if (enhancement == TimingEnhancement.SLOWER && ordinal >= Timing.TOO_LATE.ordinal()) {
            return false;
        }
        return true;
    }

    /**
     * 기본 덱 마스터 ID를 {@link BotBasicDeck#PITCH_NAMES} 순서로 반환한다.
     *
     * @return 마스터 ID 목록
     */
    private List<Long> resolveBasicMasterIds() {
        Map<String, PitchCard> byName = pitchCardRepository.findByNameIn(BotBasicDeck.PITCH_NAMES).stream()
                .collect(Collectors.toMap(PitchCard::getName, Function.identity(), (a, b) -> a));
        List<Long> ids = new ArrayList<>(BotBasicDeck.PITCH_NAMES.size());
        for (String name : BotBasicDeck.PITCH_NAMES) {
            PitchCard card = byName.get(name);
            if (card == null) {
                throw new NotFoundException(ErrorCode.PITCH_CARD_NOT_FOUND, "name=" + name);
            }
            ids.add(card.getId());
        }
        return List.copyOf(ids);
    }
}
