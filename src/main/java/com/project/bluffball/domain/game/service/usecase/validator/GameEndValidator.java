package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.redis.TurnResultSession;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 경기 종료 후 DB 아카이브 입력값 검증 (Repository·Entity 직접 접근 없음).
 */
@Component
public class GameEndValidator {

    public void validateArchiveReady(boolean initialized, boolean gameEnded, boolean alreadyArchived) {
        if (!initialized) {
            throw new IllegalStateException("경기 진행이 초기화되지 않았습니다.");
        }
        if (!gameEnded) {
            throw new IllegalStateException("경기가 종료되지 않았습니다.");
        }
        if (alreadyArchived) {
            throw new IllegalStateException("이미 DB에 저장된 경기입니다. matchSessionId 중복 저장을 방지합니다.");
        }
    }

    public void validateCompletedTurnSessions(List<TurnResultSession> sessions) {
        if (sessions.isEmpty()) {
            throw new IllegalStateException("저장할 턴 결과가 없습니다.");
        }

        for (TurnResultSession session : sessions) {
            if (session.getTurnResult() == null) {
                throw new IllegalStateException(
                        "미완료 턴이 포함되어 있습니다. turnNumber=" + session.getTurnNumber());
            }
            if (session.getSelectedPitchCardId() == null
                    || session.getSelectedCoordinateCardId() == null) {
                throw new IllegalStateException(
                        "투수 선택 정보가 없는 턴이 포함되어 있습니다. turnNumber=" + session.getTurnNumber());
            }
            if (session.getCurrentPitcherUserId() == null || session.getCurrentBatterUserId() == null) {
                throw new IllegalStateException(
                        "투수·타자 정보가 없는 턴이 포함되어 있습니다. turnNumber=" + session.getTurnNumber());
            }
        }
    }
}
