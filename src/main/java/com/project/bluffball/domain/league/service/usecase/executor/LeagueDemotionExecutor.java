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
 * 주기 점수 감쇠·강등 배치 Executor.
 *
 * <p>모든 진행 팀에 {@code PERIODIC_RATING_DECAY}를 적용한 뒤,
 * {@code rating < demoteBelow}이면 한 단계 강등한다.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeagueDemotionExecutor {

    private final TeamLeagueProgressRepository teamLeagueProgressRepository;

    /**
     * 해당 포맷에 점수 감쇠를 적용하고, 기준 미달 팀을 강등한다.
     *
     * @param format 포맷
     * @return 강등된 팀 수
     */
    @Transactional
    public int demoteIfNeeded(LeagueFormat format) {
        int demoted = 0;
        for (LeagueTier tier : LeagueTierRule.LADDER) {
            List<TeamLeagueProgress> rows =
                    teamLeagueProgressRepository.findByFormatAndCurrentTier(format, tier);
            for (TeamLeagueProgress progress : rows) {
                LeagueTier beforeTier = progress.getCurrentTier();
                progress.applyPeriodicDecay();

                LeagueTier previous = LeagueTierRule.previousTier(progress.getCurrentTier());
                if (previous != null && progress.shouldDemote()) {
                    progress.demoteTo(previous);
                    demoted++;
                    log.info("리그 강등 teamId={} format={} from={} to={} rating={}",
                            progress.getTeamId(), format, beforeTier, previous, progress.getRating());
                } else {
                    log.debug("리그 감쇠 teamId={} format={} tier={} rating={}",
                            progress.getTeamId(), format, progress.getCurrentTier(), progress.getRating());
                }
                teamLeagueProgressRepository.save(progress);
            }
        }
        return demoted;
    }
}
