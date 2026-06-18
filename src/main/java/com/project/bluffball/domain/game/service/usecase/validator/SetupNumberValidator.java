package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 블러핑 숫자 검증 전용 컴포넌트 (usecase/validator 계층).
 *
 * <p>Repository에 접근하지 않으며, 요청 값의 규칙 위반 여부만 판단한다.</p>
 *
 * <h3>검증 규칙</h3>
 * <ul>
 *   <li>모든 숫자: 1~12 범위 (Bean Validation으로 1차 처리, 여기서 2차 확인)</li>
 *   <li>각 리스트 내 중복 불가</li>
 *   <li>outNumList ∩ dpNumList = ∅ (아웃 숫자와 병살 숫자 중복 불가)</li>
 *   <li>tripleNumList ∩ hrNumList = ∅ (3루타 숫자와 홈런 숫자 중복 불가)</li>
 * </ul>
 */
@Component
public class SetupNumberValidator {

    /**
     * 제출된 블러핑 숫자 전체를 검증한다.
     *
     * @param request 검증할 요청 DTO
     * @throws IllegalArgumentException 규칙 위반 시
     */
    public void validate(SetupNumberRequest request) {
        validateRange(request.getOutNumList(), "아웃 번호");
        validateRange(request.getDpNumList(), "병살 번호");
        validateRange(request.getTripleNumList(), "3루타 번호");
        validateRange(request.getHrNumList(), "홈런 번호");

        validateNoDuplicatesInList(request.getOutNumList(), "아웃 번호");
        validateNoDuplicatesInList(request.getDpNumList(), "병살 번호");
        validateNoDuplicatesInList(request.getTripleNumList(), "3루타 번호");
        validateNoDuplicatesInList(request.getHrNumList(), "홈런 번호");

        validateNoOverlap(request.getOutNumList(), request.getDpNumList(),
                "아웃 번호", "병살 번호");
        validateNoOverlap(request.getTripleNumList(), request.getHrNumList(),
                "3루타 번호", "홈런 번호");
    }

    // ── 내부 검증 메서드 ───────────────────────────────────────────────────────────

    private static final int MIN_NUM = 1;
    private static final int MAX_NUM = 12;

    private void validateRange(List<Integer> nums, String label) {
        if (nums == null) return;
        for (Integer num : nums) {
            if (num == null || num < MIN_NUM || num > MAX_NUM) {
                throw new IllegalArgumentException(
                        label + " 숫자는 " + MIN_NUM + "~" + MAX_NUM + " 사이여야 합니다. 숫자=" + num);
            }
        }
    }

    private void validateNoDuplicatesInList(List<Integer> nums, String label) {
        if (nums == null) return;
        if (new HashSet<>(nums).size() != nums.size()) {
            throw new IllegalArgumentException(label + " 목록 내에 중복된 숫자가 있습니다.");
        }
    }

    private void validateNoOverlap(List<Integer> a, List<Integer> b,
                                   String labelA, String labelB) {
        if (a == null || b == null) return;
        Set<Integer> setA = new HashSet<>(a);
        for (Integer num : b) {
            if (setA.contains(num)) {
                throw new IllegalArgumentException(
                        labelA + "와 " + labelB + "에 중복된 숫자가 있습니다. 숫자=" + num);
            }
        }
    }
}
