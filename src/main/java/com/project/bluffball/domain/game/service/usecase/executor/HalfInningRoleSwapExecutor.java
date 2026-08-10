package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 공수 교대 시 투수·타자 역할 교환 Executor.
 */
@Component
@RequiredArgsConstructor
public class HalfInningRoleSwapExecutor {

    private final MatchInfoRepository matchInfoRepository;
    private final MatchInfoReader matchInfoReader;

    /**
     * 게임 모드에 맞게 공수 교대를 수행한다.
     *
     * @param matchSessionId 매치 세션 ID
     * @return 새 등판 투수 userId
     */
    public Long swap(String matchSessionId) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        GameMode gameMode = matchInfo.getGameMode();
        return switch (gameMode) {
            case SHOWDOWN, CUSTOM, BOT -> swapForSingleMode(matchSessionId);
            case COMPACT_LEAGUE, FULL_LEAGUE -> swapForLeague(matchSessionId);
        };
    }

    /**
     * 싱글 모드(1 vs 1)에서 공수 교대 시 투수·타자를 맞바꾼다.
     *
     * @param matchSessionId 매치 세션 ID
     * @return 새 등판 투수 userId
     */
    public Long swapForSingleMode(String matchSessionId) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        if (!matchInfo.getGameMode().usesShowdownRules()) {
            throw new BadRequestException(ErrorCode.GAME_ROLE_SWAP_UNSUPPORTED);
        }
        if (matchInfo.getBatterLineup().size() != 1) {
            throw new BadRequestException(ErrorCode.GAME_ROLE_SWAP_UNSUPPORTED);
        }

        Long newPitcherUserId = matchInfo.swapRolesForSingleMode();
        matchInfoRepository.save(matchInfo);
        return newPitcherUserId;
    }

    /**
     * 리그 모드 공수 교대 — 팀별 active 투수·타순으로 전환한다.
     *
     * <p>셋업 재제출·멀리건 없음. 타순 인덱스는 팀별로 이어간다.</p>
     *
     * @param matchSessionId 매치 세션 ID
     * @return 새 등판 투수 userId
     */
    public Long swapForLeague(String matchSessionId) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        GameMode gameMode = matchInfo.getGameMode();
        if (gameMode != GameMode.COMPACT_LEAGUE && gameMode != GameMode.FULL_LEAGUE) {
            throw new BadRequestException(ErrorCode.GAME_ROLE_SWAP_UNSUPPORTED);
        }

        Long newPitcherUserId = matchInfo.swapRolesForLeague();
        matchInfoRepository.save(matchInfo);
        return newPitcherUserId;
    }
}
