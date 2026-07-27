package com.project.bluffball.domain.game.dto.response;

/**
 * 리그 투수 교체 후 셋업 재제출 안내 이벤트.
 *
 * @param newPitcherUserId 신임 투수 (투수 셋업 필요)
 * @param demotedPitcherUserId 강판 투수 (타자 셋업 재제출 필요)
 * @param pitcherUserId 현재 등판 투수 (= newPitcherUserId)
 */
public record PitcherSubstitutedEvent(
        Long newPitcherUserId,
        Long demotedPitcherUserId,
        Long pitcherUserId
) {
}
