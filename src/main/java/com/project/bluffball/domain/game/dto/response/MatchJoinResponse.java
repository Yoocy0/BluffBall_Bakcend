package com.project.bluffball.domain.game.dto.response;



import com.project.bluffball.domain.game.enums.MatchJoinStatus;



/**

 * 매칭 큐 진입 API 응답 DTO.

 *

 * @param status          {@code WAITING} — 대기 중 / {@code MATCHED} — 즉시 매칭 성사

 * @param matchSessionId  매칭 성사 시에만 포함 (대기 중이면 {@code null})

 */

public record MatchJoinResponse(

        MatchJoinStatus status,

        String matchSessionId

) {



    /** 큐 대기 상태 응답 팩토리 */

    public static MatchJoinResponse waiting() {

        return new MatchJoinResponse(MatchJoinStatus.WAITING, null);

    }



    /** 즉시 매칭 성사 응답 팩토리 */

    public static MatchJoinResponse matched(String matchSessionId) {

        return new MatchJoinResponse(MatchJoinStatus.MATCHED, matchSessionId);

    }



    /** HTTP 202 vs 200 분기용 — 대기 중이면 {@code true} */

    public boolean isWaiting() {

        return status == MatchJoinStatus.WAITING;

    }

}


