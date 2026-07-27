package com.project.bluffball.domain.league.service;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.service.usecase.executor.LeagueMatchResultExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueMatchResultService")
class LeagueMatchResultServiceTest {

    @Mock
    private LeagueMatchResultExecutor leagueMatchResultExecutor;

    @InjectMocks
    private LeagueMatchResultService leagueMatchResultService;

    @Test
    @DisplayName("경기 결과를 Executor에 위임한다")
    void delegates() {
        leagueMatchResultService.applyMatchResult(LeagueFormat.COMPACT, 1L, 2L, 5, 3);

        verify(leagueMatchResultExecutor).applyMatchResult(LeagueFormat.COMPACT, 1L, 2L, 5, 3);
    }
}
