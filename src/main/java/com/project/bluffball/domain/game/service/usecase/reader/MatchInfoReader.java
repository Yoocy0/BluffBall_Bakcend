package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.user.record.enums.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MatchInfo Redis 엔티티 읽기 전담 리더.
 *
 * <p>Service 레이어는 이 클래스의 원시값·ID 반환 메서드만 호출한다.
 * 엔티티(MatchInfo)를 직접 반환하는 {@link #getById}는
 * Executor·Reader 내부에서만 사용한다.</p>
 */
@Component
@RequiredArgsConstructor
public class MatchInfoReader {

    private final MatchInfoRepository matchInfoRepository;
    private final GameModeRule gameModeRule;

    /** Executor·Reader 내부 전용 — Service에서 호출 금지 */
    public MatchInfo getById(String matchSessionId) {
        return matchInfoRepository.findById(matchSessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "매치를 찾을 수 없습니다. matchSessionId=" + matchSessionId));
    }

    /** 현재 투수 카드 패 ID 목록 반환 (Service ✅) */
    public List<Long> getPitcherCardHand(String matchSessionId) {
        return getById(matchSessionId).getPitcherCardHand();
    }

    /** 게임 모드 반환 (Service ✅) */
    public GameMode getGameMode(String matchSessionId) {
        return getById(matchSessionId).getGameMode();
    }

    /** 멀리건 완료 여부 반환 (Service ✅) */
    public boolean isMulliganDone(String matchSessionId) {
        return getById(matchSessionId).isMulliganDone();
    }

    /**
     * 모든 플레이어의 블러핑 숫자 제출이 완료됐는지 확인한다. (Service ✅)
     *
     * <p>outNumbers Map의 size를 모드별 필요 인원 수와 비교한다.
     * 4개 필드(out/dp/triple/hr) 중 outNumbers만으로 판단하는 이유는,
     * SetupNumberExecutor가 4개 필드를 항상 동시에 저장하기 때문이다.</p>
     */
    public boolean isSetupNumbersComplete(String matchSessionId) {
        MatchInfo matchInfo = getById(matchSessionId);
        int required = gameModeRule.getRequiredSetupCount(matchInfo.getGameMode());
        return matchInfo.getOutNumbers().size() >= required;
    }
}
