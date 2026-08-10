package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.entity.UserPitchCard;
import com.project.bluffball.domain.card.enums.EnhancementEffect;
import com.project.bluffball.domain.card.enums.TimingEnhancement;
import com.project.bluffball.domain.card.repository.UserPitchCardRepository;
import com.project.bluffball.domain.game.bot.BotDeckCatalog;
import com.project.bluffball.domain.game.enums.BotDifficulty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 봇 난이도별 구종 인벤토리 시딩·드로우 풀(인스턴스 ID) 확정 Executor.
 *
 * <p>상점 강화 카드를 소모하지 않고 Entity에 직접 단일 강화를 적용한다.
 * 풀봇 재사용을 위해 idempotent 하다.</p>
 */
@Component
@RequiredArgsConstructor
public class BotDeckInventoryExecutor {

    /** 난이도별 마스터 풀·강화 규칙 */
    private final BotDeckCatalog botDeckCatalog;

    /** 유저 구종 인스턴스 Repository */
    private final UserPitchCardRepository userPitchCardRepository;

    /**
     * 난이도에 맞게 봇 인벤토리를 보장하고 드로우/멀리건용 인스턴스 ID 풀을 반환한다.
     *
     * <ul>
     *   <li>EASY — 기본 덱 4장 기본본</li>
     *   <li>NORMAL — 전체 마스터 기본본</li>
     *   <li>HARD — 전체 마스터 기본본 + 합법 단일 강화 변형</li>
     * </ul>
     *
     * @param botUserId  봇 유저 ID
     * @param difficulty 난이도
     * @return 드로우 풀 인스턴스 ID 목록
     */
    @Transactional
    public List<Long> ensureDrawPool(Long botUserId, BotDifficulty difficulty) {
        if (difficulty == null) {
            return List.of();
        }
        return switch (difficulty) {
            case EASY, NORMAL -> ensureBasePool(botUserId, botDeckCatalog.masterPitchCardIdsFor(difficulty));
            case HARD -> ensureHardPool(botUserId);
        };
    }

    /**
     * 지정 마스터들의 기본본 인스턴스를 보장하고 ID 목록을 반환한다.
     *
     * @param botUserId 봇 유저
     * @param masterIds 마스터 ID
     * @return 기본본 인스턴스 ID
     */
    private List<Long> ensureBasePool(Long botUserId, List<Long> masterIds) {
        List<Long> pool = new ArrayList<>(masterIds.size());
        for (Long masterId : masterIds) {
            pool.add(ensureBaseInstance(botUserId, masterId));
        }
        return List.copyOf(pool);
    }

    /**
     * HARD 풀: 마스터마다 기본본 + 합법 단일 강화 인스턴스를 보장한다.
     *
     * @param botUserId 봇 유저
     * @return 인스턴스 ID 풀
     */
    private List<Long> ensureHardPool(Long botUserId) {
        List<PitchCard> masters = botDeckCatalog.findAllMasters();
        List<Long> pool = new ArrayList<>();
        for (PitchCard master : masters) {
            Long masterId = master.getId();
            pool.add(ensureBaseInstance(botUserId, masterId));
            for (EnhancementEffect effect : BotDeckCatalog.legalSingleEnhancements(master.getTiming())) {
                pool.add(ensureSingleEnhancedInstance(botUserId, masterId, effect));
            }
        }
        return List.copyOf(pool);
    }

    /**
     * 마스터의 미강화 기본본을 찾거나 생성한다.
     *
     * @param botUserId 봇 유저
     * @param masterId  마스터 ID
     * @return 기본본 인스턴스 ID
     */
    private Long ensureBaseInstance(Long botUserId, Long masterId) {
        List<UserPitchCard> owned = userPitchCardRepository.findByUserIdAndCardId(botUserId, masterId);
        return findBase(owned)
                .map(UserPitchCard::getId)
                .orElseGet(() -> userPitchCardRepository.save(new UserPitchCard(botUserId, masterId)).getId());
    }

    /**
     * 단일 강화 변형 인스턴스를 찾거나 생성한다 (강화 카드 미소모).
     *
     * @param botUserId 봇 유저
     * @param masterId  마스터 ID
     * @param effect    단일 강화 효과
     * @return 인스턴스 ID
     */
    private Long ensureSingleEnhancedInstance(Long botUserId, Long masterId, EnhancementEffect effect) {
        List<UserPitchCard> owned = userPitchCardRepository.findByUserIdAndCardId(botUserId, masterId);
        Optional<UserPitchCard> existing = findSingleEnhanced(owned, effect);
        if (existing.isPresent()) {
            return existing.get().getId();
        }
        UserPitchCard created = new UserPitchCard(botUserId, masterId);
        applySingleEnhancement(created, effect);
        return userPitchCardRepository.save(created).getId();
    }

    /**
     * Entity에 단일 강화를 직접 적용한다.
     *
     * @param card   인스턴스
     * @param effect 효과
     */
    private void applySingleEnhancement(UserPitchCard card, EnhancementEffect effect) {
        switch (effect) {
            case CHANGE_AMOUNT_PLUS_1 -> card.enhanceChangeAmount();
            case TIMING_FASTER -> card.enhanceTiming(TimingEnhancement.FASTER);
            case TIMING_SLOWER -> card.enhanceTiming(TimingEnhancement.SLOWER);
        }
    }

    /**
     * 미강화 기본본을 찾는다.
     *
     * @param owned 보유 목록
     * @return 기본본
     */
    private static Optional<UserPitchCard> findBase(List<UserPitchCard> owned) {
        return owned.stream().filter(UserPitchCard::isBaseCopy).findFirst();
    }

    /**
     * 지정 단일 강화만 적용된 인스턴스를 찾는다.
     *
     * @param owned  보유 목록
     * @param effect 효과
     * @return 매칭 인스턴스
     */
    private static Optional<UserPitchCard> findSingleEnhanced(List<UserPitchCard> owned, EnhancementEffect effect) {
        return owned.stream().filter(card -> matchesSingleEnhancement(card, effect)).findFirst();
    }

    /**
     * 인스턴스가 해당 단일 강화 변형인지 판정한다.
     *
     * @param card   인스턴스
     * @param effect 효과
     * @return 일치하면 true
     */
    private static boolean matchesSingleEnhancement(UserPitchCard card, EnhancementEffect effect) {
        return switch (effect) {
            case CHANGE_AMOUNT_PLUS_1 ->
                    card.isChangeAmountEnhanced() && card.getTimingEnhancement() == TimingEnhancement.NONE;
            case TIMING_FASTER ->
                    !card.isChangeAmountEnhanced() && card.getTimingEnhancement() == TimingEnhancement.FASTER;
            case TIMING_SLOWER ->
                    !card.isChangeAmountEnhanced() && card.getTimingEnhancement() == TimingEnhancement.SLOWER;
        };
    }
}
