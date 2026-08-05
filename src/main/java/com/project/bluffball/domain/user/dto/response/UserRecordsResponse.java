package com.project.bluffball.domain.user.dto.response;

import java.util.List;

/**
 * 유저 전적(타자·투수 기록) 응답.
 *
 * <p>모드·리그별 {@link BatterRecordResponse} / {@link PitcherRecordResponse} 목록을 담는다.</p>
 */
public record UserRecordsResponse(
        Long userId,
        String nickname,
        List<BatterRecordResponse> batterRecords,
        List<PitcherRecordResponse> pitcherRecords
) {
}
