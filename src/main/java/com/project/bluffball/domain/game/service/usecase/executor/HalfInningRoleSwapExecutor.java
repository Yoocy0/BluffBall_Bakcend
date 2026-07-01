package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import com.project.bluffball.domain.user.record.enums.GameMode;
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
     * 싱글 모드(1 vs 1)에서 공수 교대 시 투수·타자를 맞바꾼다.
     *
     * @return 새 등판 투수 userId
     */
    public Long swapForSingleMode(String matchSessionId) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        if (matchInfo.getGameMode() != GameMode.GENERAL) {
            throw new IllegalStateException(
                    "싱글 모드(GENERAL)에서만 공수 교대 역할 교환이 지원됩니다.");
        }
        if (matchInfo.getBatterLineup().size() != 1) {
            throw new IllegalStateException(
                    "1 vs 1 매치에서만 공수 교대 역할 교환이 지원됩니다.");
        }

        Long newPitcherUserId = matchInfo.swapRolesForSingleMode();
        matchInfoRepository.save(matchInfo);
        return newPitcherUserId;
    }
}
