package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 리그 매치 결과 반영 플래그 Executor.
 */
@Component
@RequiredArgsConstructor
public class LeagueMatchResultMarkExecutor {

    private final MatchInfoRepository matchInfoRepository;
    private final MatchInfoReader matchInfoReader;

    /**
     * 리그 결과 반영 완료를 표시한다.
     *
     * @param matchSessionId 매치 세션 ID
     */
    public void markApplied(String matchSessionId) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        matchInfo.markLeagueResultApplied();
        matchInfoRepository.save(matchInfo);
    }
}
