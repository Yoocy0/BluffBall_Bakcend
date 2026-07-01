package com.project.bluffball.domain.game.service.usecase.validator;



import com.project.bluffball.domain.game.enums.MatchJoinDecision;

import com.project.bluffball.global.exception.MatchConflictException;

import com.project.bluffball.global.exception.MatchNotFoundException;

import org.springframework.stereotype.Component;



/**

 * 매칭 큐 상태 검증 전용 컴포넌트.

 *

 * <p>Repository에 접근하지 않으며, Reader가 전달한 원시값만 검증한다.</p>

 */

@Component

public class MatchQueueValidator {



    /**

     * 큐 진입 가능 여부를 검증한다.

     *

     * @param alreadyInQueue {@link com.project.bluffball.domain.game.service.usecase.reader.MatchQueueReader#hasQueueEntry} 결과

     * @throws MatchConflictException 이미 큐에 등록된 경우 (409)

     */

    public void validateJoinAllowed(boolean alreadyInQueue) {

        if (alreadyInQueue) {

            throw new MatchConflictException("이미 매칭 큐에 등록된 유저입니다.");

        }

    }



    /**

     * 큐 poll 결과에 따른 진입 분기를 결정한다.

     *

     * <p>상대가 없거나 자기 자신이 pop된 경우 {@link MatchJoinDecision#WAIT},

     * 유효한 상대가 있는 경우 {@link MatchJoinDecision#MATCH}를 반환한다.</p>

     *

     * @param opponentUserId Redis 큐에서 pop된 상대 userId — 없으면 {@code null}

     * @param joinerUserId   지금 큐에 진입한 유저 ID

     * @return 대기({@code WAIT}) 또는 즉시 매칭({@code MATCH}) 분기

     */

    public MatchJoinDecision resolveJoinDecision(Long opponentUserId, Long joinerUserId) {

        if (opponentUserId == null) {

            return MatchJoinDecision.WAIT;

        }

        if (opponentUserId.equals(joinerUserId)) {

            return MatchJoinDecision.WAIT;

        }

        return MatchJoinDecision.MATCH;

    }



    /**

     * 큐 취소 요청 가능 여부를 검증한다.

     *

     * @param hasQueueEntry 큐 등록 존재 여부

     * @param queueState    현재 큐 상태 — 등록 없으면 {@code null}

     * @throws MatchNotFoundException 큐에 등록되지 않은 경우 (404)

     * @throws MatchConflictException 이미 매칭이 성사된 경우 (409)

     */

    public void validateCancelRequest(boolean hasQueueEntry) {

        if (!hasQueueEntry) {

            throw new MatchNotFoundException("매칭 큐에 등록된 유저가 아닙니다.");

        }

    }



    /**

     * 매칭 성사 처리 전 선매칭 유저의 큐 등록 존재 여부를 검증한다.

     *

     * @param hasOpponentEntry 선매칭 유저의 {@link com.project.bluffball.domain.game.redis.MatchQueueEntry} 존재 여부

     * @throws MatchNotFoundException 등록 정보가 없는 경우 (404)

     */

    public void validateOpponentEntryExists(boolean hasOpponentEntry) {

        if (!hasOpponentEntry) {

            throw new MatchNotFoundException("매칭 큐 등록 정보를 찾을 수 없습니다.");

        }

    }

}


