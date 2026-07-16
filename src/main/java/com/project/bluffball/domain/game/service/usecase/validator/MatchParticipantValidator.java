package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.global.exception.AuthForbiddenException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 매치 참가자 검증 전용 컴포넌트.
 *
 * <p>Repository에 접근하지 않으며, Reader가 전달한 참가 여부만 검증한다.</p>
 */
@Component
public class MatchParticipantValidator {

    /**
     * 요청 유저가 해당 매치의 참가자인지 검증한다.
     *
     * @param isParticipant {@link com.project.bluffball.domain.game.service.usecase.reader.MatchInfoReader#isParticipant} 결과
     * @throws AuthForbiddenException 참가자가 아니거나 매치가 존재하지 않을 때
     */
    public void validateParticipant(boolean isParticipant) {
        if (!isParticipant) {
            throw new AuthForbiddenException(ErrorCode.MATCH_NOT_PARTICIPANT);
        }
    }
}
