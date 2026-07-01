package com.project.bluffball.domain.game.enums;



/**

 * 매칭 큐 등록 상태.

 *

 * <p>{@link com.project.bluffball.domain.game.redis.MatchQueueEntry}에 저장된다.</p>

 */

public enum MatchQueueState {



    /** 큐에서 상대를 기다리는 중 */

    WAITING,



    /** 매칭 성사 — matchSessionId 발급 완료 */

    MATCHED

}


