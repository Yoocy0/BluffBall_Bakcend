package com.project.bluffball.domain.card.bootstrap;

import com.project.bluffball.domain.card.entity.CoordinateCard;
import com.project.bluffball.domain.card.enums.UserType;
import com.project.bluffball.domain.card.repository.CoordinateCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좌표 카드 마스터 데이터 초기화.
 *
 * <p>1~25: 투수 시작 좌표·타자 예측 좌표 공용 ({@link UserType#BOTH}).<br>
 * 0: 타자 전용 폭투 존 ({@link UserType#BATTER}) — 투수는 선택 불가, 최종 좌표로만 등장.</p>
 */
@Component
@RequiredArgsConstructor
public class CoordinateCardInitializer implements ApplicationRunner {

    private final CoordinateCardRepository coordinateCardRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (coordinateCardRepository.count() > 0) {
            return;
        }

        for (int coordinateNumber = 1; coordinateNumber <= 25; coordinateNumber++) {
            coordinateCardRepository.save(new CoordinateCard(
                    "좌표 " + coordinateNumber,
                    UserType.BOTH,
                    coordinateNumber,
                    isStrikeZone(coordinateNumber)));
        }

        coordinateCardRepository.save(new CoordinateCard(
                "폭투 존 (좌표 0)",
                UserType.BATTER,
                0,
                false));
    }

    /**
     * 5×5 격자 중앙 3×3(9칸)을 스트라이크 존으로 본다.
     *
     * <pre>
     *  1  2  3  4  5
     *  6  7  8  9 10
     * 11 12 13 14 15
     * 16 17 18 19 20
     * 21 22 23 24 25
     * </pre>
     */
    private static boolean isStrikeZone(int coordinateNumber) {
        int y = (coordinateNumber - 1) / 5;
        int x = (coordinateNumber - 1) % 5;
        return x >= 1 && x <= 3 && y >= 1 && y <= 3;
    }
}
