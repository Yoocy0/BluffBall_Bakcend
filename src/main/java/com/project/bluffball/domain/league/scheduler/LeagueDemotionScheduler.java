package com.project.bluffball.domain.league.scheduler;

import com.project.bluffball.domain.league.service.LeagueDemotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 리그 강등 스케줄러 (KST).
 *
 * <p>5분마다 폴링한다. Compact=매주 월, Full=매월 1일 경계를 처리한다.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeagueDemotionScheduler {

    private final LeagueDemotionService leagueDemotionService;

    /**
     * 강등 주기를 폴링한다.
     */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void runDemotionTick() {
        try {
            leagueDemotionService.tick();
        } catch (Exception e) {
            log.error("리그 강등 스케줄 실패", e);
        }
    }
}
