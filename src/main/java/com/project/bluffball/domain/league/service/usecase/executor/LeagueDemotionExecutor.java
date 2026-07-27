package com.project.bluffball.domain.league.service.usecase.executor;

import com.project.bluffball.domain.league.config.LeagueTierRule;
import com.project.bluffball.domain.league.entity.TeamLeagueProgress;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.repository.TeamLeagueProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주기 강등 배치 Executor.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeagueDemotionExecutor {

    private final TeamLeagueProgressRepository teamLeagueProgressRepository;

    /**
     * 해당 포맷에서 강등 기준 미달 팀을 한 단계 내린다.
     *
     * @param format 포맷
     * @return 강등된 팀 수
     */
    @Transactional
    public int demoteIfNeeded(LeagueFormat format) {
        int demoted = 0;
        for (LeagueTier tier : LeagueTierRule.LADDER) {
            if (LeagueTierRule.previousTier(tier) == null) {
                continue;
            }
            List<TeamLeagueProgress> rows =
                    teamLeagueProgressRepository.findByFormatAndCurrentTier(format, tier);
            for (TeamLeagueProgress progress : rows) {
                if (!progress.shouldDemote()) {
                    continue;
                }
                LeagueTier previous = LeagueTierRule.previousTier(progress.getCurrentTier());
                if (previous == null) {
                    continue;
                }
                progress.demoteTo(previous);
                teamLeagueProgressRepository.save(progress);
                demoted++;
                log.info("리그 강등 teamId={} format={} from={} to={} rating={}",
                        progress.getTeamId(), format, tier, previous, progress.getRating());
            }
        }
        return demoted;
    }
}
