package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 블러핑 숫자 검증 전용 컴포넌트 (usecase/validator 계층).
 */
@Component
public class SetupNumberValidator {

    private static final int MIN_NUM = 1;
    private static final int MAX_NUM = 12;

    public void validate(SetupNumberRequest request) {
        if (request == null) {
            throw new BadRequestException(ErrorCode.REQUEST_BODY_EMPTY);
        }
        validateRequired(request.outNumList(), "아웃 번호", 5);
        validateRequired(request.dpNumList(), "병살 번호", 1);
        validateRequired(request.tripleNumList(), "3루타 번호", 1);
        validateRequired(request.hrNumList(), "홈런 번호", 1);

        validateRange(request.outNumList(), "아웃 번호");
        validateRange(request.dpNumList(), "병살 번호");
        validateRange(request.tripleNumList(), "3루타 번호");
        validateRange(request.hrNumList(), "홈런 번호");

        validateNoDuplicatesInList(request.outNumList(), "아웃 번호");
        validateNoDuplicatesInList(request.dpNumList(), "병살 번호");
        validateNoDuplicatesInList(request.tripleNumList(), "3루타 번호");
        validateNoDuplicatesInList(request.hrNumList(), "홈런 번호");

        validateNoOverlap(request.outNumList(), request.dpNumList(), "아웃 번호", "병살 번호");
        validateNoOverlap(request.tripleNumList(), request.hrNumList(), "3루타 번호", "홈런 번호");
    }

    private void validateRequired(List<Integer> nums, String label, int expectedSize) {
        if (nums == null || nums.isEmpty()) {
            throw new BadRequestException(ErrorCode.GAME_INVALID_SETUP_NUMBERS, label + " 목록이 비어 있습니다.");
        }
        if (nums.size() != expectedSize) {
            throw new BadRequestException(
                    ErrorCode.GAME_INVALID_SETUP_NUMBERS,
                    label + "는 " + expectedSize + "개여야 합니다. actual=" + nums.size());
        }
    }

    private void validateRange(List<Integer> nums, String label) {
        if (nums == null) {
            return;
        }
        for (Integer num : nums) {
            if (num == null || num < MIN_NUM || num > MAX_NUM) {
                throw new BadRequestException(
                        ErrorCode.GAME_INVALID_SETUP_NUMBERS,
                        label + " 숫자는 " + MIN_NUM + "~" + MAX_NUM + " 사이여야 합니다. 숫자=" + num);
            }
        }
    }

    private void validateNoDuplicatesInList(List<Integer> nums, String label) {
        if (nums == null) {
            return;
        }
        if (new HashSet<>(nums).size() != nums.size()) {
            throw new BadRequestException(
                    ErrorCode.GAME_INVALID_SETUP_NUMBERS, label + " 목록 내에 중복된 숫자가 있습니다.");
        }
    }

    private void validateNoOverlap(List<Integer> a, List<Integer> b, String labelA, String labelB) {
        if (a == null || b == null) {
            return;
        }
        Set<Integer> setA = new HashSet<>(a);
        for (Integer num : b) {
            if (setA.contains(num)) {
                throw new BadRequestException(
                        ErrorCode.GAME_INVALID_SETUP_NUMBERS,
                        labelA + "와 " + labelB + "에 중복된 숫자가 있습니다. 숫자=" + num);
            }
        }
    }
}
