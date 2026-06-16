package com.project.bluffball.domain.game.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 블러핑 고유 숫자 선택 WebSocket 수신 DTO.
 *
 * <p>게임 시작 직후(카드 드로우 전) 양측이 제출하는 비밀 숫자 설정 요청이다.
 * 선택한 숫자는 주사위 결과 판정 시 특수 결과를 발동시키는 트리거로 사용된다.</p>
 *
 * <ul>
 *   <li><b>투수 역할</b>: 아웃 유발 번호(5개) + 병살 유발 번호(1개)를 선택한다.
 *       주사위 결과가 이 번호들과 일치하면 타자에게 불리한 판정이 발생한다.</li>
 *   <li><b>타자 역할</b>: 3루타 번호(1개) + 홈런 번호(1개)를 선택한다.
 *       주사위 결과가 이 번호들과 일치하면 타자에게 유리한 판정이 발생한다.</li>
 *   <li><b>싱글 모드</b>: 한 유저가 투수/타자를 모두 담당하므로 4개 필드를 동시에 제출한다.</li>
 *   <li><b>팀전 모드</b>: 투수 유저는 pitcher 필드만, 타자 유저는 batter 필드만 제출한다.</li>
 * </ul>
 *
 * <p>모든 숫자는 1~12 범위 내에서 선택하며, 같은 역할 내에서 중복 선택은 불가하다.</p>
 */
@Getter
@NoArgsConstructor
public class SetupNumberRequest {

    /**
     * 투수가 지정하는 아웃 유발 번호 목록 (5개 고정).
     * 주사위 결과가 이 중 하나와 일치하면 '아웃' 판정이 발생한다.
     */
    @Size(min = 5, max = 5, message = "아웃 번호는 정확히 5개 선택해야 합니다.")
    private List<@Min(1) @Max(12) Integer> pitcherOutNumbers;

    /**
     * 투수가 지정하는 병살 유발 번호 (1개 고정).
     * 주자가 있는 상황에서 주사위 결과가 이 번호와 일치하면 '병살타' 판정이 발생한다.
     */
    @Min(value = 1, message = "번호는 1 이상이어야 합니다.")
    @Max(value = 12, message = "번호는 12 이하여야 합니다.")
    private Integer pitcherDoublePlayNumber;

    /**
     * 타자가 지정하는 3루타 번호 (1개 고정).
     * 주사위 결과가 이 번호와 일치하면 '3루타' 판정이 발생한다.
     * 서버는 이 값을 {@code MatchInfo.batterTripleNumbers}에 요청자의 userId를 키로 저장한다.
     */
    @Min(value = 1, message = "번호는 1 이상이어야 합니다.")
    @Max(value = 12, message = "번호는 12 이하여야 합니다.")
    private Integer batterTripleNumber;

    /**
     * 타자가 지정하는 홈런 번호 (1개 고정).
     * 주사위 결과가 이 번호와 일치하면 '홈런' 판정이 발생한다.
     * 서버는 이 값을 {@code MatchInfo.batterHomerunNumbers}에 요청자의 userId를 키로 저장한다.
     */
    @Min(value = 1, message = "번호는 1 이상이어야 합니다.")
    @Max(value = 12, message = "번호는 12 이하여야 합니다.")
    private Integer batterHomerunNumber;
}
