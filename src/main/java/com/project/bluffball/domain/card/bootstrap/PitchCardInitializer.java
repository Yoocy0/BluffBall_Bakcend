package com.project.bluffball.domain.card.bootstrap;

import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.card.repository.PitchCardRepository;
import com.project.bluffball.domain.game.enums.Timing;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 구종 카드 마스터 데이터 초기화.
 *
 * <p>기동 시 마스터 구종이 없으면 등록한다. 이미 DB에 구종이 있어도 신규 구종만 추가한다.</p>
 */
@Component
@RequiredArgsConstructor
public class PitchCardInitializer implements ApplicationRunner {

    private final PitchCardRepository pitchCardRepository;

    private record PitchSeed(String name, int changeAmount, ChangeDirection direction, Timing timing) {
    }

    private static final List<PitchSeed> MASTER_PITCHES = List.of(
            new PitchSeed("포심 패스트볼", 0, ChangeDirection.DOWN, Timing.EARLY),
            new PitchSeed("커터", 1, ChangeDirection.SIDE, Timing.EARLY),
            new PitchSeed("스플리터", 1, ChangeDirection.DOWN, Timing.EARLY),
            new PitchSeed("슬라이더", 2, ChangeDirection.SIDE, Timing.NORMAL),
            new PitchSeed("커브", 3, ChangeDirection.DOWN, Timing.LATE),
            new PitchSeed("포크", 2, ChangeDirection.DOWN, Timing.NORMAL)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (PitchSeed seed : MASTER_PITCHES) {
            ensurePitch(seed);
        }
    }

    private void ensurePitch(PitchSeed seed) {
        if (pitchCardRepository.existsByName(seed.name())) {
            return;
        }
        pitchCardRepository.save(new PitchCard(
                seed.name(),
                seed.changeAmount(),
                seed.direction(),
                seed.timing()));
    }
}
