package com.project.bluffball.domain.card.bootstrap;

import com.project.bluffball.domain.card.entity.EnhancementCard;
import com.project.bluffball.domain.card.enums.EnhancementEffect;
import com.project.bluffball.domain.card.repository.EnhancementCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 강화 카드 마스터 데이터 초기화 (3종).
 */
@Component
@Order(15)
@RequiredArgsConstructor
public class EnhancementCardInitializer implements ApplicationRunner {

    private final EnhancementCardRepository enhancementCardRepository;

    private record EnhancementSeed(String name, EnhancementEffect effect) {
    }

    private static final List<EnhancementSeed> MASTER_ENHANCEMENTS = List.of(
            new EnhancementSeed("변화량 강화", EnhancementEffect.CHANGE_AMOUNT_PLUS_1),
            new EnhancementSeed("타이밍 가속", EnhancementEffect.TIMING_FASTER),
            new EnhancementSeed("타이밍 감속", EnhancementEffect.TIMING_SLOWER)
    );

    /**
     * 없는 강화 카드만 등록한다.
     *
     * @param args 기동 인자
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (EnhancementSeed seed : MASTER_ENHANCEMENTS) {
            ensure(seed);
        }
    }

    private void ensure(EnhancementSeed seed) {
        if (enhancementCardRepository.existsByEffect(seed.effect())) {
            return;
        }
        if (enhancementCardRepository.existsByName(seed.name())) {
            return;
        }
        enhancementCardRepository.save(new EnhancementCard(seed.name(), seed.effect()));
    }
}
