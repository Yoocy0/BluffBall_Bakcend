package com.project.bluffball.gametest.bootstrap;

import com.project.bluffball.domain.card.service.UserPitchCardService;
import com.project.bluffball.domain.user.entity.User;
import com.project.bluffball.gametest.service.GameTestDevLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게임 테스트 개발자 계정·전 구종 인벤토리 초기화.
 *
 * <p>기동 시 {@code DevTester} 유저를 보장하고 미보유 마스터 구종 기본본을 모두 지급한다.
 * {@link com.project.bluffball.domain.card.bootstrap.PitchCardInitializer} 이후에 실행한다.</p>
 */
@Component
@DependsOn("pitchCardInitializer")
@Order(200)
@RequiredArgsConstructor
@Slf4j
public class GameTestDevUserInitializer implements ApplicationRunner {

    private final GameTestDevLoginService gameTestDevLoginService;
    private final UserPitchCardService userPitchCardService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User user = gameTestDevLoginService.ensureDevUser();
        int before = userPitchCardService.getMyCards(user.getId()).size();
        userPitchCardService.acquireAllMissing(user.getId());
        int after = userPitchCardService.getMyCards(user.getId()).size();
        log.info("game-test DevTester ready userId={} pitchCards {} -> {}", user.getId(), before, after);
    }
}
