package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.config.GameModeRule;
import com.project.bluffball.domain.game.redis.MatchInfo;
import com.project.bluffball.domain.game.repository.MatchInfoRepository;
import com.project.bluffball.domain.user.record.enums.GameMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        MatchInfo matchInfo = matchInfoRepository.findById(matchSessionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "매치를 찾을 수 없습니다. matchSessionId=" + matchSessionId));
        matchInfo.ensureCollectionsInitialized();
        return matchInfo;
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

    /** 현재 등판 투수 userId 반환 (Service ✅) */
    public Long getPitcherUserId(String matchSessionId) {
        return getById(matchSessionId).getPitcherUserId();
    }

    /** 현재 타석 타자 userId 반환 (Service ✅) */
    public Long getCurrentBatterUserId(String matchSessionId) {
        MatchInfo matchInfo = getById(matchSessionId);
        List<Long> lineup = matchInfo.getBatterLineup();
        if (lineup.isEmpty()) {
            throw new IllegalStateException("타순이 비어 있습니다. matchSessionId=" + matchSessionId);
        }
        return lineup.get(matchInfo.getCurrentBatterIndex());
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

    /** 투수 아웃 유발 블러핑 숫자 (Executor·Calculator 내부용) */
    public List<Integer> getOutNumbers(String matchSessionId, Long pitcherUserId) {
        return getBluffingNumbers(getById(matchSessionId).getOutNumbers(), pitcherUserId);
    }

    /** 투수 병살 유발 블러핑 숫자 (Executor·Calculator 내부용) */
    public List<Integer> getDpNumbers(String matchSessionId, Long pitcherUserId) {
        return getBluffingNumbers(getById(matchSessionId).getDpNumbers(), pitcherUserId);
    }

    /** 타자 3루타 유발 블러핑 숫자 (Executor·Calculator 내부용) */
    public List<Integer> getTripleNumbers(String matchSessionId, Long batterUserId) {
        return getBluffingNumbers(getById(matchSessionId).getTripleNumbers(), batterUserId);
    }

    /** 타자 홈런 유발 블러핑 숫자 (Executor·Calculator 내부용) */
    public List<Integer> getHrNumbers(String matchSessionId, Long batterUserId) {
        return getBluffingNumbers(getById(matchSessionId).getHrNumbers(), batterUserId);
    }

    private List<Integer> getBluffingNumbers(Map<Long, List<Integer>> numbersByUserId, Long userId) {
        List<Integer> numbers = numbersByUserId.get(userId);
        return numbers != null ? numbers : Collections.emptyList();
    }
}
