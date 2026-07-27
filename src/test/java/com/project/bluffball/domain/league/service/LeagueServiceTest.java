package com.project.bluffball.domain.league.service;

import com.project.bluffball.domain.league.dto.response.LeagueResponse;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueService")
class LeagueServiceTest {

    @Mock
    private LeagueReader leagueReader;

    @InjectMocks
    private LeagueService leagueService;

    @Test
    @DisplayName("카탈로그 목록·상세를 Reader에 위임한다")
    void delegates() {
        LeagueResponse item = new LeagueResponse(
                1L, LeagueFormat.COMPACT, LeagueTier.AMATEUR_4, "아마4", 1000L, 0L);
        when(leagueReader.getLeagues(LeagueFormat.COMPACT, null)).thenReturn(List.of(item));
        when(leagueReader.getLeagueResponse(1L)).thenReturn(item);

        assertThat(leagueService.getLeagues(LeagueFormat.COMPACT, null)).containsExactly(item);
        assertThat(leagueService.getLeague(1L)).isEqualTo(item);
        verify(leagueReader).getLeagues(LeagueFormat.COMPACT, null);
        verify(leagueReader).getLeagueResponse(1L);
    }
}
