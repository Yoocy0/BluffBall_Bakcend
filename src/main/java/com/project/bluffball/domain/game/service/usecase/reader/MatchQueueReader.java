package com.project.bluffball.domain.game.service.usecase.reader;

import com.project.bluffball.domain.game.enums.MatchQueueState;
import com.project.bluffball.domain.game.redis.MatchQueueEntry;
import com.project.bluffball.domain.game.repository.MatchQueueEntryRepository;
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

    /** 유저별 큐 등록 정보 Redis Repository */
    private final MatchQueueEntryRepository matchQueueEntryRepository;

    /**
     * 유저의 큐 등록 존재 여부를 반환한다.
     *
     * @param userId 조회 대상 유저 ID
     * @return 등록되어 있으면 {@code true}
     */
    public boolean hasQueueEntry(Long userId) {
        return matchQueueEntryRepository.existsById(userId);
    }

    /**
     * 유저의 현재 큐 상태를 반환한다.
     *
     * @param userId 조회 대상 유저 ID
     * @return {@link MatchQueueState} — 등록 없으면 {@code null}
     */
    public MatchQueueState getQueueState(Long userId) {
        return matchQueueEntryRepository.findById(userId)
                .map(MatchQueueEntry::getState)
                .orElse(null);
    }

    /**
     * 큐 등록 Entity를 조회한다.
     *
     * <p>Executor·Reader 내부 전용 — Service에서 호출 금지.</p>
     *
     * @param userId 조회 대상 유저 ID
     * @return {@link MatchQueueEntry}
     * @throws MatchNotFoundException 등록 정보가 없을 때
     */
    public MatchQueueEntry getById(Long userId) {
        return matchQueueEntryRepository.findById(userId)
                .orElseThrow(() -> new MatchNotFoundException(
                        "매칭 큐 등록 정보를 찾을 수 없습니다. userId=" + userId));
    }
}
