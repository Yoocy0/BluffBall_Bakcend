package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.redis.GameState;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.GameStateRepository;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.team.dto.response.TeamPitchCardsResponse;
import com.project.bluffball.domain.user.record.enums.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * 리그 매치 생성 Executor.
 *
 * <p>MatchInfo·GameState를 Redis에 seed하고 matchSessionId만 반환한다.</p>
 */
@Component
@RequiredArgsConstructor
public class LeagueMatchCreateExecutor {

    private final MatchInfoRepository matchInfoRepository;
    private final GameStateRepository gameStateRepository;

    /**
     * 리그 매치를 생성한다. 선진입 팀이 홈이며, 홈 선발 투수로 시작한다.
     *
     * @param gameMode COMPACT_LEAGUE / FULL_LEAGUE
     * @param leagueTier 리그 단계
     * @param homeTeamId 홈 팀 ID
     * @param awayTeamId 어웨이 팀 ID
     * @param homeLeaderUserId 홈 리더 userId
     * @param awayLeaderUserId 어웨이 리더 userId
     * @param homeStartingPitcherUserId 홈 선발 투수
     * @param awayStartingPitcherUserId 어웨이 선발 투수 (공수 교대 시 사용)
     * @param homeBattingOrder 홈 타순
     * @param awayBattingOrder 어웨이 타순
     * @param homePitchCards 홈 구종 사전 선택
     * @param awayPitchCards 어웨이 구종 사전 선택
     * @return matchSessionId
     */
    public String execute(
            GameMode gameMode,
            LeagueTier leagueTier,
            Long homeTeamId,
            Long awayTeamId,
            Long homeLeaderUserId,
            Long awayLeaderUserId,
            Long homeStartingPitcherUserId,
            Long awayStartingPitcherUserId,
            List<Long> homeBattingOrder,
            List<Long> awayBattingOrder,
            TeamPitchCardsResponse homePitchCards,
            TeamPitchCardsResponse awayPitchCards) {

        String matchSessionId = UUID.randomUUID().toString();

        MatchInfo matchInfo = MatchInfo.createLeague(
                matchSessionId,
                gameMode,
                leagueTier,
                homeTeamId,
                awayTeamId,
                homeLeaderUserId,
                awayLeaderUserId,
                homeStartingPitcherUserId,
                awayStartingPitcherUserId,
                homeBattingOrder,
                awayBattingOrder);
        matchInfo.ensureCollectionsInitialized();
        matchInfo.initializeDoubleJudgmentSettings();

        applyPitchCards(matchInfo, homePitchCards);
        applyPitchCards(matchInfo, awayPitchCards);
        matchInfo.syncPitcherCardHandFromPlayer();

        GameState gameState = GameState.builder()
                .id(matchSessionId)
                .build();

        matchInfoRepository.save(matchInfo);
        gameStateRepository.save(gameState);
        return matchSessionId;
    }

    /**
     * 사전 선택 카드를 MatchInfo 플레이어 핸드에 반영한다.
     *
     * @param matchInfo 매치 정보
     * @param pitchCards 팀 사전 선택
     */
    private void applyPitchCards(MatchInfo matchInfo, TeamPitchCardsResponse pitchCards) {
        if (pitchCards == null || pitchCards.selections() == null) {
            return;
        }
        for (TeamPitchCardsResponse.MemberPitchCards selection : pitchCards.selections()) {
            matchInfo.setPlayerCardHand(selection.userId(), selection.userPitchCardIds());
            matchInfo.setPlayerDropCard(selection.userId(), selection.dropCardId());
        }
    }
}
