package com.project.bluffball.domain.game.service.usecase.executor;

import com.project.bluffball.domain.game.entity.InningLog;
import com.project.bluffball.domain.game.redis.TurnResultSession;
import com.project.bluffball.domain.game.repository.InningLogRepository;
import com.project.bluffball.domain.game.repository.TurnResultSessionRepository;
import com.project.bluffball.domain.game.service.usecase.reader.CoordinateCardReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 경기 종료 시 Redis TurnResultSession → MariaDB InningLog 영구 저장 Executor.
 */
@Component
@RequiredArgsConstructor
public class GameEndExecutor {

    private final InningLogRepository inningLogRepository;
    private final TurnResultSessionRepository turnResultSessionRepository;
    private final CoordinateCardReader coordinateCardReader;

    /**
     * 완료된 턴 세션을 InningLog로 변환·저장하고 Redis 세션을 삭제한다.
     *
     * @param sessions {@link com.project.bluffball.domain.game.service.usecase.reader.GameEndReader}에서
     *                 조회·검증된 완료 턴 목록
     */
    @Transactional
    public void archive(List<TurnResultSession> sessions) {
        List<InningLog> logs = sessions.stream()
                .map(this::toInningLog)
                .toList();
        inningLogRepository.saveAll(logs);

        for (TurnResultSession session : sessions) {
            turnResultSessionRepository.deleteById(session.getId());
        }
    }

    private InningLog toInningLog(TurnResultSession session) {
        Long batterCoordinateCardId = session.getSelectedBatterCoordinateCardId();
        if (batterCoordinateCardId == null) {
            batterCoordinateCardId = coordinateCardReader.getCoordinateCardId(
                    session.getBatterSelectedCoordinateNumber());
        }

        return new InningLog(
                session.getMatchSessionId(),
                session.getTurnNumber(),
                session.getCurrentPitcherUserId(),
                session.getCurrentBatterUserId(),
                session.getInning(),
                session.isTop(),
                session.getSelectedPitchCardId(),
                session.getSelectedCoordinateCardId(),
                batterCoordinateCardId,
                session.getSelectedTiming(),
                session.getTurnResult());
    }
}
