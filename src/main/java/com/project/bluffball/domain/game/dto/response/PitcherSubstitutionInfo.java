package com.project.bluffball.domain.game.dto.response;

import java.util.List;

/**
 * 리그 투수 교체 상태 (현재 수비 팀 기준).
 *
 * @param used 이미 사용한 교체 횟수
 * @param max 모드별 최대 교체 횟수 (Compact 1 / Full 3 / 그 외 0)
 * @param candidateUserIds 교체 가능한 신임 투수 후보
 * @param myTeamDefending 요청 유저가 현재 수비 로스터에 속하는지
 */
public record PitcherSubstitutionInfo(
        int used,
        int max,
        List<Long> candidateUserIds,
        boolean myTeamDefending
) {
}
