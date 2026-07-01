package com.project.bluffball.domain.game.dto.response;



/**

 * 매칭 성사 WebSocket 알림 DTO.

 *

 * <p>대기 중이던 유저(선매칭)에게 {@code /topic/user/{userId}/match}로 전송된다.

 * 수신 후 클라이언트는 {@code matchSessionId}로 게임 WebSocket에 연결한다.</p>

 *

 * @param matchSessionId 생성된 매치 세션 ID

 */

public record MatchFoundEvent(

        String matchSessionId

) {

}


