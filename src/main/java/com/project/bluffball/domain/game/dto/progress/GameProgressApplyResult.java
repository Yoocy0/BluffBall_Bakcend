package com.project.bluffball.domain.game.dto.progress;

import com.project.bluffball.domain.game.dto.response.GameStateSnapshot;
import com.project.bluffball.domain.game.enums.Timing;
import com.project.bluffball.domain.game.enums.TurnResult;

import java.util.List;

/**
 * 턴 결과 반영 후 {@link com.project.bluffball.domain.game.service.GameProgressService} 반환 DTO.
 *
 * <p>경기 종료 시 Redis TurnResultSession이 삭제되므로, 클라이언트 응답용 판정 정보는
 * 아카이브 전에 이 DTO에 담아 반환한다.</p>
 *
 * @param snapshot              갱신된 스코어보드 스냅샷
 * @param gameOver              경기 종료 여부 — true이면 {@code GameEndEvent} 발행 대상
 * @param turnResult            보정 반영 후 최종 판정
 * @param finalCoordinateNumber 투수 구종 최종 좌표
 * @param pitchTiming           투구 타이밍
 * @param diceResults           주사위 눈금 목록
 * @param completedTurnNumber   이번에 완료된 턴 번호
 */
public record GameProgressApplyResult(
        GameStateSnapshot snapshot,
        boolean gameOver,
        TurnResult turnResult,
        int finalCoordinateNumber,
        Timing pitchTiming,
        List<Integer> diceResults,
        int completedTurnNumber
) {
}
