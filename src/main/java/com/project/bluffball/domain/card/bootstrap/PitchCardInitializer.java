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

/**
 * 구종 카드 마스터 데이터 초기화.
 *
 * <p>앱 최초 기동 시 {@link PitchCard}가 없으면 MVP 구종 4종을 등록한다.</p>
 */
@Component
@RequiredArgsConstructor
public class PitchCardInitializer implements ApplicationRunner {

    private final PitchCardRepository pitchCardRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (pitchCardRepository.count() > 0) {
            return;
        }

        pitchCardRepository.save(new PitchCard(
                "포심 패스트볼",
                0,
                ChangeDirection.DOWN,
                Timing.EARLY));

        pitchCardRepository.save(new PitchCard(
                "슬라이더",
                2,
                ChangeDirection.SIDE,
                Timing.NORMAL));

        pitchCardRepository.save(new PitchCard(
                "커브",
                3,
                ChangeDirection.DOWN,
                Timing.LATE));

        pitchCardRepository.save(new PitchCard(
                "포크",
                2,
                ChangeDirection.DOWN,
                Timing.NORMAL));
    }
}
