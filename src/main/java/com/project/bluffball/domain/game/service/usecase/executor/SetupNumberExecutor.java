package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 블러핑 숫자 저장 전용 컴포넌트 (usecase/executor 계층).
 *
 * <p>MatchInfo의 블러핑 숫자 Map에 유저별 숫자 목록을 추가하고 Redis에 저장한다.
 * MatchInfo는 JPA 관리 엔티티가 아닌 Redis POJO이므로
 * 조회한 인스턴스의 Map을 직접 수정하고 재저장하는 방식을 사용한다.</p>
 */
@Component
@RequiredArgsConstructor
public class SetupNumberExecutor {

    private final MatchInfoRepository matchInfoRepository;

    /**
     * 유저의 블러핑 숫자를 MatchInfo에 추가하고 Redis에 저장한다.
     *
     * <p>각 Map에 userId를 키로 추가하므로
     * 다른 유저가 이미 저장한 데이터는 그대로 유지된다.</p>
     *
     * @param matchSessionId 매치 세션 ID
     * @param userId         제출한 유저 ID
     * @param request        블러핑 숫자 요청 DTO
     */
    public void save(String matchSessionId, Long userId, SetupNumberRequest request) {
        MatchInfo matchInfo = matchInfoRepository.findById(matchSessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "매치를 찾을 수 없습니다. matchSessionId=" + matchSessionId));

        matchInfo.getOutNumbers().put(userId, request.getOutNumList());
        matchInfo.getDpNumbers().put(userId, request.getDpNumList());
        matchInfo.getTripleNumbers().put(userId, request.getTripleNumList());
        matchInfo.getHrNumbers().put(userId, request.getHrNumList());

        matchInfoRepository.save(matchInfo);
    }
}
