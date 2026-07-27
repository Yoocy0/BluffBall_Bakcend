package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.enums.SetupKind;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 블러핑 숫자 저장 전용 컴포넌트 (usecase/executor 계층).
 */
@Component
@RequiredArgsConstructor
public class SetupNumberExecutor {

    private final MatchInfoRepository matchInfoRepository;
    private final MatchInfoReader matchInfoReader;

    /**
     * 유저의 블러핑 숫자를 종류별로 병합 저장한다.
     *
     * @param matchSessionId 매치 세션 ID
     * @param userId 제출 유저 ID
     * @param setupKind 제출 종류
     * @param request 블러핑 숫자 요청
     */
    public void save(
            String matchSessionId,
            Long userId,
            SetupKind setupKind,
            SetupNumberRequest request) {
        MatchInfo matchInfo = matchInfoReader.getById(matchSessionId);
        matchInfo.upsertPlayerSetupNumbers(userId, setupKind, request);
        matchInfoRepository.save(matchInfo);
    }
}
