package com.project.bluffball.domain.game.service;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.service.usecase.executor.SetupNumberExecutor;
import com.project.bluffball.domain.game.service.usecase.validator.SetupNumberValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 인게임 흐름 서비스.
 *
 * <p>Entity·Repository에 직접 접근하지 않는다.
 * 모든 검증은 Validator, 상태 변경은 Executor에 위임한다.</p>
 */
@Service
@RequiredArgsConstructor
public class GameService {

    private final SetupNumberValidator setupNumberValidator;
    private final SetupNumberExecutor setupNumberExecutor;

    /**
     * 블러핑 숫자 제출을 처리한다.
     *
     * @param matchSessionId 매치 세션 ID
     * @param userId         제출한 유저 ID
     * @param request        블러핑 숫자 요청 DTO
     */
    public void setupNumbers(String matchSessionId, Long userId, SetupNumberRequest request) {
        setupNumberValidator.validate(request);
        setupNumberExecutor.save(matchSessionId, userId, request);
    }
}
