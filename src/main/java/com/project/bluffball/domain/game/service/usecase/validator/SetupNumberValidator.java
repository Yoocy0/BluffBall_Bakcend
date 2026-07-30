package com.project.bluffball.domain.game.service.usecase.validator;

import com.project.bluffball.domain.game.dto.request.SetupNumberRequest;
import com.project.bluffball.domain.game.enums.SetupKind;
import com.project.bluffball.global.exception.BadRequestException;
import com.project.bluffball.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 블러핑 숫자 검증 전용 컴포넌트 (usecase/validator 계층).
 *
 * <p>쇼다운은 4종 일괄, 리그는 역할별(투수 OUT·병살 / 타자 3루타·홈런)로 검증한다.</p>
 */
@Component
public class SetupNumberValidator {

    private static final int MIN_NUM = 1;
    private static final int MAX_NUM = 12;
    /** 홈런은 주사위 2개 합 구간만 허용 */
    private static final int HR_MIN_NUM = 7;
    private static final int HR_MAX_NUM = 12;

    /** 투수 아웃 번호 개수 */
    public static final int OUT_COUNT = 5;

    /** 투수 병살 번호 개수 */
    public static final int DP_COUNT = 1;

    /** 타자 3루타 번호 개수 */
    public static final int TRIPLE_COUNT = 1;

    /** 타자 홈런 번호 개수 */
    public static final int HR_COUNT = 1;

    /**
     * 셋업 종류에 맞게 요청을 검증한다.
     *
     * @param request 블러핑 숫자 요청
     * @param setupKind 제출 종류
     * @throws BadRequestException 규칙 위반 시
     */
    public void validate(SetupNumberRequest request, SetupKind setupKind) {
        if (request == null) {
            throw new BadRequestException(ErrorCode.REQUEST_BODY_EMPTY);
        }
        if (setupKind == null) {
            throw new BadRequestException(ErrorCode.GAME_INVALID_SETUP_NUMBERS, "setupKind is null");
        }
        switch (setupKind) {
            case FULL -> validateFull(request);
            case PITCHER -> validatePitcher(request);
            case BATTER -> validateBatter(request);
        }
    }

    /**
     * 쇼다운 일괄 제출을 검증한다.
     *
     * @param request 요청
     */
    private void validateFull(SetupNumberRequest request) {
        validateRequired(request.outNumList(), "아웃 번호", OUT_COUNT);
        validateRequired(request.dpNumList(), "병살 번호", DP_COUNT);
        validateRequired(request.tripleNumList(), "3루타 번호", TRIPLE_COUNT);
        validateRequired(request.hrNumList(), "홈런 번호", HR_COUNT);

        validateRange(request.outNumList(), "아웃 번호");
        validateRange(request.dpNumList(), "병살 번호");
        validateRange(request.tripleNumList(), "3루타 번호");
        validateRange(request.hrNumList(), "홈런 번호", HR_MIN_NUM, HR_MAX_NUM);

        validateNoDuplicatesInList(request.outNumList(), "아웃 번호");
        validateNoDuplicatesInList(request.dpNumList(), "병살 번호");
        validateNoDuplicatesInList(request.tripleNumList(), "3루타 번호");
        validateNoDuplicatesInList(request.hrNumList(), "홈런 번호");

        validateNoOverlap(request.outNumList(), request.dpNumList(), "아웃 번호", "병살 번호");
        validateNoOverlap(request.tripleNumList(), request.hrNumList(), "3루타 번호", "홈런 번호");
    }

    /**
     * 리그 투수 셋업(OUT·병살)을 검증한다.
     *
     * @param request 요청
     */
    private void validatePitcher(SetupNumberRequest request) {
        validateRequired(request.outNumList(), "아웃 번호", OUT_COUNT);
        validateRequired(request.dpNumList(), "병살 번호", DP_COUNT);
        validateRange(request.outNumList(), "아웃 번호");
        validateRange(request.dpNumList(), "병살 번호");
        validateNoDuplicatesInList(request.outNumList(), "아웃 번호");
        validateNoDuplicatesInList(request.dpNumList(), "병살 번호");
        validateNoOverlap(request.outNumList(), request.dpNumList(), "아웃 번호", "병살 번호");
    }

    /**
     * 리그 타자 셋업(3루타·홈런)을 검증한다.
     *
     * @param request 요청
     */
    private void validateBatter(SetupNumberRequest request) {
        validateRequired(request.tripleNumList(), "3루타 번호", TRIPLE_COUNT);
        validateRequired(request.hrNumList(), "홈런 번호", HR_COUNT);
        validateRange(request.tripleNumList(), "3루타 번호");
        validateRange(request.hrNumList(), "홈런 번호", HR_MIN_NUM, HR_MAX_NUM);
        validateNoDuplicatesInList(request.tripleNumList(), "3루타 번호");
        validateNoDuplicatesInList(request.hrNumList(), "홈런 번호");
        validateNoOverlap(request.tripleNumList(), request.hrNumList(), "3루타 번호", "홈런 번호");
    }

    /**
     * 목록이 비어 있지 않고 기대 개수와 일치하는지 검증한다.
     *
     * @param nums 숫자 목록
     * @param label 오류 메시지용 라벨
     * @param expectedSize 기대 개수
     */
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

    /**
     * 숫자 범위(1~12)를 검증한다.
     *
     * @param nums 숫자 목록
     * @param label 라벨
     */
    private void validateRange(List<Integer> nums, String label) {
        validateRange(nums, label, MIN_NUM, MAX_NUM);
    }

    /**
     * 숫자 범위를 검증한다.
     *
     * @param nums 숫자 목록
     * @param label 라벨
     * @param min 최소값(포함)
     * @param max 최대값(포함)
     */
    private void validateRange(List<Integer> nums, String label, int min, int max) {
        if (nums == null) {
            return;
        }
        for (Integer num : nums) {
            if (num == null || num < min || num > max) {
                throw new BadRequestException(
                        ErrorCode.GAME_INVALID_SETUP_NUMBERS,
                        label + " 숫자는 " + min + "~" + max + " 사이여야 합니다. 숫자=" + num);
            }
        }
    }

    /**
     * 목록 내 중복을 검증한다.
     *
     * @param nums 숫자 목록
     * @param label 라벨
     */
    private void validateNoDuplicatesInList(List<Integer> nums, String label) {
        if (nums == null) {
            return;
        }
        if (new HashSet<>(nums).size() != nums.size()) {
            throw new BadRequestException(
                    ErrorCode.GAME_INVALID_SETUP_NUMBERS, label + " 목록 내에 중복된 숫자가 있습니다.");
        }
    }

    /**
     * 두 목록 간 중복을 검증한다.
     *
     * @param a 목록 A
     * @param b 목록 B
     * @param labelA 라벨 A
     * @param labelB 라벨 B
     */
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
