package com.project.bluffball.domain.league.bootstrap;

import com.project.bluffball.domain.league.service.LeagueDemotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 기동 시 강등 주기가 도래했으면 한 번 실행한다.
 *
 * <p>{@link LeagueCatalogInitializer} 이후에 실행한다.</p>
 */
@Component
@Order(110)
@RequiredArgsConstructor
public class LeagueDemotionBootstrap implements ApplicationRunner {

    private final LeagueDemotionService leagueDemotionService;

    @Override
    public void run(ApplicationArguments args) {
        leagueDemotionService.tick();
    }
}
