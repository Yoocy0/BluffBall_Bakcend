package com.project.bluffball.domain.league.service;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.service.usecase.executor.LeagueDemotionExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueDemotionService")
class LeagueDemotionServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private LeagueDemotionExecutor leagueDemotionExecutor;

    @Test
    @DisplayName("월요일(1일 아님)이면 Compact만 강등")
    void mondayOnlyCompact() {
        // 2026-07-27 = Monday, not day 1
        Clock clock = Clock.fixed(Instant.parse("2026-07-26T15:00:00Z"), KST);
        LeagueDemotionService service = new LeagueDemotionService(leagueDemotionExecutor, clock);
        when(leagueDemotionExecutor.demoteIfNeeded(LeagueFormat.COMPACT)).thenReturn(2);

        int total = service.tick();

        assertThat(total).isEqualTo(2);
        verify(leagueDemotionExecutor).demoteIfNeeded(LeagueFormat.COMPACT);
        verify(leagueDemotionExecutor, never()).demoteIfNeeded(LeagueFormat.FULL);
    }

    @Test
    @DisplayName("같은 날 두 번 tick해도 Compact는 1회만")
    void oncePerDay() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-26T15:00:00Z"), KST);
        LeagueDemotionService service = new LeagueDemotionService(leagueDemotionExecutor, clock);
        when(leagueDemotionExecutor.demoteIfNeeded(LeagueFormat.COMPACT)).thenReturn(1);

        assertThat(service.tick()).isEqualTo(1);
        assertThat(service.tick()).isZero();
        verify(leagueDemotionExecutor, times(1)).demoteIfNeeded(LeagueFormat.COMPACT);
    }

    @Test
    @DisplayName("매월 1일(월요일이 아니면) Full만 강등")
    void firstOfMonthOnlyFull() {
        // 2026-07-01 = Wednesday
        assertThat(LocalDate.of(2026, 7, 1).getDayOfWeek().name()).isEqualTo("WEDNESDAY");
        Clock clock = Clock.fixed(Instant.parse("2026-06-30T15:00:00Z"), KST);
        LeagueDemotionService service = new LeagueDemotionService(leagueDemotionExecutor, clock);
        when(leagueDemotionExecutor.demoteIfNeeded(LeagueFormat.FULL)).thenReturn(3);

        int total = service.tick();

        assertThat(total).isEqualTo(3);
        verify(leagueDemotionExecutor).demoteIfNeeded(LeagueFormat.FULL);
        verify(leagueDemotionExecutor, never()).demoteIfNeeded(LeagueFormat.COMPACT);
    }

    @Test
    @DisplayName("월요일+1일이면 Compact·Full 둘 다")
    void mondayAndFirst() {
        // 2026-06-01 = Monday
        Clock clock = Clock.fixed(Instant.parse("2026-05-31T15:00:00Z"), KST);
        LeagueDemotionService service = new LeagueDemotionService(leagueDemotionExecutor, clock);
        when(leagueDemotionExecutor.demoteIfNeeded(LeagueFormat.COMPACT)).thenReturn(1);
        when(leagueDemotionExecutor.demoteIfNeeded(LeagueFormat.FULL)).thenReturn(2);

        assertThat(service.tick()).isEqualTo(3);
        verify(leagueDemotionExecutor).demoteIfNeeded(LeagueFormat.COMPACT);
        verify(leagueDemotionExecutor).demoteIfNeeded(LeagueFormat.FULL);
    }
}
