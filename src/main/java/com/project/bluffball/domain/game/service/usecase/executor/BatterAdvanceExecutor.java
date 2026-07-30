package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.enums.TurnResult;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 타석 종료 시 타순 전진 Executor.
 */
@Component
@RequiredArgsConstructor
public class BatterAdvanceExecutor {

    private final MatchInfoRepository matchInfoRepository;
    private final MatchInfoReader matchInfoReader;

    /**
     * 타석이 끝난 판정이면 타순을 한 칸 전진한다.
     *
     * @param matchSessionId 매치 세션 ID
     * @param turnResult 턴 판정
     * @return 전진했으면 새 타자 userId, 아니면 null
     */
    public Long advanceIfPlateAppearanceEnded(String matchSessionId, TurnResult turnResult) {
        if (!isPlateAppearanceEnded(turnResult)) {
            return null;
        }
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        // 쇼다운(타자 1명)도 모듈로 순환 — 동작상 동일 타자 유지
        Long nextBatter = matchInfo.advanceBatterAfterPlateAppearance();
        matchInfoRepository.save(matchInfo);
        return nextBatter;
    }

    /**
     * 타석이 종료되는 판정인지 반환한다.
     *
     * @param turnResult 턴 판정
     * @return 타석 종료면 true
     */
    private boolean isPlateAppearanceEnded(TurnResult turnResult) {
        return switch (turnResult) {
            case SINGLE, DOUBLE, TRIPLE, HOMERUN, WALK, STRIKE_OUT, OUT, DOUBLE_PLAY -> true;
            case STRIKE, BALL, WILD_PITCH, FOUL -> false;
        };
    }
}
