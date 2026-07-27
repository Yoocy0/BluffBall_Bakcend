package com.project.bluffball.domain.league.service;

import com.project.bluffball.domain.league.dto.response.LeagueResponse;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.service.usecase.reader.LeagueReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 리그 카탈로그 서비스.
 *
 * <p>Entity·Repository에 직접 접근하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class LeagueService {

    private final LeagueReader leagueReader;

    /**
     * 리그 카탈로그 목록을 조회한다.
     *
     * @param format 포맷 필터
     * @param tier 티어 필터
     * @return 리그 목록
     */
    public List<LeagueResponse> getLeagues(LeagueFormat format, LeagueTier tier) {
        return leagueReader.getLeagues(format, tier);
    }

    /**
     * 리그 등급 상세를 조회한다.
     *
     * @param leagueId 리그 ID
     * @return 리그 상세
     */
    public LeagueResponse getLeague(Long leagueId) {
        return leagueReader.getLeagueResponse(leagueId);
    }
}
