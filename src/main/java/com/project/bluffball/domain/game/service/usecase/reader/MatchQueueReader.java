package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.enums.MatchQueueState;
import com.project.bluffball.domain.game.redis.MatchQueueEntry;
import com.project.bluffball.domain.game.repository.MatchQueueEntryRepository;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.user.record.enums.GameMode;
import com.project.bluffball.global.exception.ErrorCode;
import com.project.bluffball.global.exception.MatchNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 매칭 큐 등록 상태 읽기 전담 리더.
 *
 * <p>Service는 원시값·enum 반환 메서드만 호출한다.
 * Entity 반환 {@link #getById}는 Executor 내부에서만 사용한다.</p>
 */
@Component
@RequiredArgsConstructor
public class MatchQueueReader {

    private final MatchQueueEntryRepository matchQueueEntryRepository;

    public boolean hasQueueEntry(Long userId) {
        return matchQueueEntryRepository.existsById(userId);
    }

    public MatchQueueState getQueueState(Long userId) {
        return matchQueueEntryRepository.findById(userId)
                .map(MatchQueueEntry::getState)
                .orElse(null);
    }

    /**
     * 큐에 등록된 팀 ID를 반환한다. (리그 매칭용)
     *
     * @param userId 유저 ID
     * @return 팀 ID — 없으면 null
     */
    public Long getTeamId(Long userId) {
        return matchQueueEntryRepository.findById(userId)
                .map(MatchQueueEntry::getTeamId)
                .orElse(null);
    }

    /**
     * 큐에 등록된 리그 티어를 반환한다.
     *
     * @param userId 유저 ID
     * @return 리그 티어 — 없으면 null
     */
    public LeagueTier getLeagueTier(Long userId) {
        return matchQueueEntryRepository.findById(userId)
                .map(MatchQueueEntry::getLeagueTier)
                .orElse(null);
    }

    /**
     * 큐에 등록된 게임 모드를 반환한다.
     *
     * @param userId 유저 ID
     * @return 게임 모드 — 없으면 null
     */
    public GameMode getGameMode(Long userId) {
        return matchQueueEntryRepository.findById(userId)
                .map(MatchQueueEntry::getGameMode)
                .orElse(null);
    }

    /**
     * 큐 등록 Entity를 조회한다.
     *
     * <p>Executor·Reader 내부 전용 — Service에서 호출 금지.</p>
     *
     * @param userId 유저 ID
     * @return 큐 Entry
     */
    public MatchQueueEntry getById(Long userId) {
        return matchQueueEntryRepository.findById(userId)
                .orElseThrow(() -> new MatchNotFoundException(ErrorCode.MATCH_NOT_FOUND));
    }
}
