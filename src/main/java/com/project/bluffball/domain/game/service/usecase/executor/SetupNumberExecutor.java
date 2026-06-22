package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 블러핑 숫자 저장 전용 컴포넌트 (usecase/executor 계층).
 *
 * <p>Service·Reader에서 조회한 {@link MatchInfo}에 숫자를 반영하고 Redis에 저장한다.
 * 매치 조회·존재 검증은 호출 전 Service에서 완료된 상태여야 한다.</p>
 */
@Component
@RequiredArgsConstructor
public class SetupNumberExecutor {

    private final MatchInfoRepository matchInfoRepository;

    /**
     * 유저의 블러핑 숫자를 MatchInfo에 추가하고 Redis에 저장한다.
     *
     * @param matchInfo Service에서 Reader로 조회한 매치 정보
     * @param userId    제출한 유저 ID
     * @param request   블러핑 숫자 요청 DTO
     */
    public void save(MatchInfo matchInfo, Long userId, SetupNumberRequest request) {
        matchInfo.getOutNumbers().put(userId, request.getOutNumList());
        matchInfo.getDpNumbers().put(userId, request.getDpNumList());
        matchInfo.getTripleNumbers().put(userId, request.getTripleNumList());
        matchInfo.getHrNumbers().put(userId, request.getHrNumList());

        matchInfoRepository.save(matchInfo);
    }
}
