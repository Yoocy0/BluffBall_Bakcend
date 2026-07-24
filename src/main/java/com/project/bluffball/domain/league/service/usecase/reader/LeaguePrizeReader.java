package com.project.bluffball.domain.league.service.usecase.reader;

import com.project.bluffball.domain.league.dto.response.LeaguePrizeItemResponse;
import com.project.bluffball.domain.league.dto.response.LeaguePrizeResponse;
import com.project.bluffball.domain.league.entity.LeaguePrizeRule;
import com.project.bluffball.domain.league.repository.LeaguePrizeRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 리그 상금표 읽기 전담 Reader (usecase/reader 계층).
 */
@Component
@RequiredArgsConstructor
public class LeaguePrizeReader {

    private final LeaguePrizeRuleRepository leaguePrizeRuleRepository;
    private final LeagueSeasonReader leagueSeasonReader;
    private final LeagueReader leagueReader;

    /**
     * 시즌(소속 리그) 기준 순위별 상금표를 반환한다.
     *
     * @param seasonId 시즌 ID
     * @return 상금표 응답 DTO
     */
    public LeaguePrizeResponse getPrizes(Long seasonId) {
        Long leagueId = leagueSeasonReader.getLeagueId(seasonId);
        long firstPlacePrize = leagueReader.getFirstPlacePrize(leagueId);

        List<LeaguePrizeRule> rules = leaguePrizeRuleRepository.findAllByOrderByRankPositionAsc();
        List<LeaguePrizeItemResponse> prizes = rules.stream()
                .map(rule -> new LeaguePrizeItemResponse(
                        rule.getRankPosition(),
                        rule.getPercentOfFirst(),
                        firstPlacePrize * rule.getPercentOfFirst() / 100L))
                .toList();

        return new LeaguePrizeResponse(seasonId, leagueId, firstPlacePrize, prizes);
    }
}
