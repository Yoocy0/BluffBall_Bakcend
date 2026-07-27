package com.project.bluffball.domain.league.bootstrap;

import com.project.bluffball.domain.league.entity.League;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.repository.LeagueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리그 카탈로그 시드.
 *
 * <p>참가비는 티어별 placeholder이며 밸런스 조정 시 이 클래스만 수정한다.</p>
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class LeagueCatalogInitializer implements ApplicationRunner {

    private final LeagueRepository leagueRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedLeagues(LeagueFormat.COMPACT);
        seedLeagues(LeagueFormat.FULL);
    }

    /**
     * 포맷별 7티어 리그를 시드한다.
     *
     * @param format 리그 구분
     */
    private void seedLeagues(LeagueFormat format) {
        int minMembers = format == LeagueFormat.COMPACT ? 4 : 9;
        for (LeagueTier tier : LeagueTier.values()) {
            leagueRepository.findByFormatAndTier(format, tier).ifPresentOrElse(
                    league -> {
                        if (league.getMinTeamMembers() != minMembers) {
                            league.updateMinTeamMembers(minMembers);
                            leagueRepository.save(league);
                        }
                    },
                    () -> {
                        int tierIndex = tier.ordinal() + 1;
                        long entryFee = 1_000L * tierIndex;
                        String name = formatLabel(format) + " " + tierLabel(tier);
                        leagueRepository.save(
                                new League(format, tier, name, entryFee, 0L, minMembers));
                    });
        }
    }

    /**
     * 포맷 표시명.
     *
     * @param format 포맷
     * @return 표시명
     */
    private String formatLabel(LeagueFormat format) {
        return switch (format) {
            case COMPACT -> "컴팩트 리그";
            case FULL -> "풀 리그";
        };
    }

    /**
     * 티어 표시명.
     *
     * @param tier 티어
     * @return 표시명
     */
    private String tierLabel(LeagueTier tier) {
        return switch (tier) {
            case AMATEUR_1 -> "아마 1부";
            case AMATEUR_2 -> "아마 2부";
            case AMATEUR_3 -> "아마 3부";
            case AMATEUR_4 -> "아마 4부";
            case INDEPENDENT -> "독립";
            case PRO_1 -> "프로 1부";
            case PRO_2 -> "프로 2부";
        };
    }
}
