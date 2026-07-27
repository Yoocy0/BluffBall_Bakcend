package com.project.bluffball.domain.league.service;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.service.usecase.executor.LeagueDemotionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 리그 강등 주기 서비스.
 *
 * <p>Compact=매주 월요일, Full=매월 1일에 한 번씩 강등 판정을 수행한다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeagueDemotionService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final LeagueDemotionExecutor leagueDemotionExecutor;

    /** Compact 마지막 강등 일자 (KST) */
    private final AtomicReference<LocalDate> lastCompactDemotionDate = new AtomicReference<>();
    /** Full 마지막 강등 일자 (KST) */
    private final AtomicReference<LocalDate> lastFullDemotionDate = new AtomicReference<>();

    /**
     * 강등 주기가 도래했으면 포맷별로 실행한다.
     *
     * @return 강등된 총 팀 수
     */
    public int tick() {
        LocalDateTime now = LocalDateTime.now(KST);
        LocalDate today = now.toLocalDate();
        int total = 0;

        if (shouldRunCompact(today) && markOnce(lastCompactDemotionDate, today)) {
            int n = leagueDemotionExecutor.demoteIfNeeded(LeagueFormat.COMPACT);
            total += n;
            log.info("Compact 리그 강등 완료 demoted={} date={}", n, today);
        }
        if (shouldRunFull(today) && markOnce(lastFullDemotionDate, today)) {
            int n = leagueDemotionExecutor.demoteIfNeeded(LeagueFormat.FULL);
            total += n;
            log.info("Full 리그 강등 완료 demoted={} date={}", n, today);
        }
        return total;
    }

    /**
     * Compact 강등일(월요일)인지.
     *
     * @param today KST 날짜
     * @return 월요일이면 true
     */
    private boolean shouldRunCompact(LocalDate today) {
        return today.getDayOfWeek() == DayOfWeek.MONDAY;
    }

    /**
     * Full 강등일(매월 1일)인지.
     *
     * @param today KST 날짜
     * @return 1일이면 true
     */
    private boolean shouldRunFull(LocalDate today) {
        return today.getDayOfMonth() == 1;
    }

    /**
     * 당일 1회만 실행되도록 표시한다.
     *
     * @param ref 마지막 실행일 보관
     * @param today 오늘
     * @return 이번이 첫 실행이면 true
     */
    private boolean markOnce(AtomicReference<LocalDate> ref, LocalDate today) {
        LocalDate prev = ref.get();
        if (today.equals(prev)) {
            return false;
        }
        return ref.compareAndSet(prev, today);
    }
}
