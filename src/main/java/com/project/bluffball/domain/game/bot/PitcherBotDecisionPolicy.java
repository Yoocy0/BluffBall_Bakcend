package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.card.enums.ChangeDirection;
import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.BotDifficulty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 투수 봇(EASY/NORMAL/HARD) 투구 판단 정책.
 *
 * <ul>
 *   <li>유리(스트라이크 &gt; 볼) — EASY/NORMAL: 변화구(비패스트볼), 변화량 큰 것 우선</li>
 *   <li>불리·동점(스트라이크 ≤ 볼) — EASY/NORMAL: 패스트볼; 없으면 난이도별 대체</li>
 *   <li>EASY — 최종 좌표가 항상 스트라이크존</li>
 *   <li>NORMAL — 유리 시 유인구(존 밖), 불리 시 존 승부</li>
 *   <li>HARD — 카운트별 옵션 우선순위 + 40/30/20/10 가중 샘플링</li>
 * </ul>
 */
@Component
public class PitcherBotDecisionPolicy {

    /** 스트라이크존 후보 시작 좌표 (중앙 우선) */
    private static final int[] ZONE_STARTS = {13, 8, 12, 14, 18, 7, 9, 17, 19};

    /** 존 밖·가장자리 시작 좌표 (유인구용) */
    private static final int[] EDGE_STARTS = {
            1, 2, 3, 4, 5, 6, 10, 11, 15, 16, 20, 21, 22, 23, 24, 25
    };

    /** 전체 격자 시작 좌표 (존 후보로 부족할 때 폴백) */
    private static final int[] ALL_STARTS = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25
    };

    /** HARD 유리 카운트 옵션 우선순위(높→낮) — 타자 HARD가 동일 표를 재사용 */
    static final List<PitcherBotOptionKind> HARD_FAVORABLE_PRIORITY = List.of(
            PitcherBotOptionKind.BREAKING_BALL,
            PitcherBotOptionKind.FASTBALL_STRIKE,
            PitcherBotOptionKind.FASTBALL_BALL,
            PitcherBotOptionKind.BREAKING_STRIKE
    );

    /** HARD 불리·동점 카운트 옵션 우선순위(높→낮) — 타자 HARD가 동일 표를 재사용 */
    static final List<PitcherBotOptionKind> HARD_UNFAVORABLE_PRIORITY = List.of(
            PitcherBotOptionKind.FASTBALL_STRIKE,
            PitcherBotOptionKind.BREAKING_STRIKE,
            PitcherBotOptionKind.BREAKING_BALL,
            PitcherBotOptionKind.FASTBALL_BALL
    );

    /** HARD 실현 가능 옵션에 순서대로 부여하는 가중치 접두 */
    static final int[] HARD_WEIGHTS = {40, 30, 20, 10};

    /** HARD 샘플링용 난수 (테스트에서 시드/모킹 주입) */
    private final Random random;

    /**
     * 기본 난수로 정책을 생성한다(Spring 빈).
     */
    public PitcherBotDecisionPolicy() {
        this(new Random());
    }

    /**
     * 난수를 주입해 정책을 생성한다.
     *
     * @param random 샘플링용 난수(null이면 새 Random)
     */
    PitcherBotDecisionPolicy(Random random) {
        this.random = random != null ? random : new Random();
    }

    /**
     * 현재 핸드·카운트·난이도로 투구를 결정한다.
     *
     * @param hand       투수 핸드 구종(실효 스탯)
     * @param balls      볼 카운트
     * @param strikes    스트라이크 카운트
     * @param difficulty EASY / NORMAL / HARD
     * @return 구종·시작 좌표
     * @throws IllegalArgumentException 핸드가 비었거나 난이도가 null이면
     */
    public PitcherBotThrowDecision decide(
            List<CardInfo> hand,
            int balls,
            int strikes,
            BotDifficulty difficulty) {
        if (hand == null || hand.isEmpty()) {
            throw new IllegalArgumentException("pitcher hand is empty");
        }
        if (difficulty == null) {
            throw new IllegalArgumentException("difficulty is null");
        }
        if (difficulty == BotDifficulty.HARD) {
            return decideHard(hand, balls, strikes);
        }

        boolean favorable = strikes > balls;
        CardInfo pitch = favorable
                ? pickBreaking(hand)
                : pickFastballOrSubstitute(hand, difficulty);
        int start = pickStartCoordinate(pitch, favorable, difficulty);

        return new PitcherBotThrowDecision(pitch.cardId(), start);
    }

    /**
     * HARD: 실현 가능 옵션을 우선순위로 모은 뒤 가중 샘플링한다.
     *
     * @param hand    핸드
     * @param balls   볼
     * @param strikes 스트라이크
     * @return 투구 결정
     */
    private PitcherBotThrowDecision decideHard(List<CardInfo> hand, int balls, int strikes) {
        List<PitcherBotHardOption> options = planHardOptions(hand, balls, strikes);
        if (options.isEmpty()) {
            // 이론상 드묾 — 핸드 첫 장 + 중앙
            CardInfo fallback = hand.get(0);
            return new PitcherBotThrowDecision(fallback.cardId(), 13);
        }
        PitcherBotHardOption picked = sampleHardOption(options);
        return new PitcherBotThrowDecision(picked.pitch().cardId(), picked.startCoordinateNumber());
    }

    /**
     * HARD용 실현 가능 옵션 목록(우선순위 순, 접두 가중치 부여)을 만든다.
     *
     * <p>실현 불가 종류는 건너뛰고, 남은 N개에 {@code {40,30,20,10}} 접두를 부여한다.</p>
     *
     * @param hand    핸드
     * @param balls   볼
     * @param strikes 스트라이크
     * @return 가중 옵션(비어 있을 수 있음)
     */
    List<PitcherBotHardOption> planHardOptions(List<CardInfo> hand, int balls, int strikes) {
        boolean favorable = strikes > balls;
        List<PitcherBotOptionKind> priority =
                favorable ? HARD_FAVORABLE_PRIORITY : HARD_UNFAVORABLE_PRIORITY;

        List<PitcherBotHardOption> realizable = new ArrayList<>();
        for (PitcherBotOptionKind kind : priority) {
            PitcherBotHardOption option = tryRealizeHardOption(hand, kind);
            if (option != null) {
                realizable.add(option);
            }
        }

        List<PitcherBotHardOption> weighted = new ArrayList<>(realizable.size());
        for (int i = 0; i < realizable.size(); i++) {
            PitcherBotHardOption base = realizable.get(i);
            weighted.add(new PitcherBotHardOption(
                    base.kind(),
                    base.pitch(),
                    base.startCoordinateNumber(),
                    HARD_WEIGHTS[i]));
        }
        return weighted;
    }

    /**
     * 옵션 종류를 핸드에서 구종·시작 좌표로 실현한다. 불가하면 null.
     *
     * @param hand 핸드
     * @param kind 옵션 종류
     * @return 가중치 0인 후보, 실현 불가면 null
     */
    private PitcherBotHardOption tryRealizeHardOption(List<CardInfo> hand, PitcherBotOptionKind kind) {
        boolean wantFastball = kind == PitcherBotOptionKind.FASTBALL_STRIKE
                || kind == PitcherBotOptionKind.FASTBALL_BALL;
        boolean wantInZone = kind == PitcherBotOptionKind.FASTBALL_STRIKE
                || kind == PitcherBotOptionKind.BREAKING_STRIKE;

        // 패스트볼 부재 시 NORMAL과 동일한 대체 순위
        CardInfo pitch = wantFastball
                ? pickFastballOrSubstitute(hand, BotDifficulty.NORMAL)
                : pickBreaking(hand);

        Integer start = wantInZone
                ? findMatchingStart(pitch, true, ZONE_STARTS, ALL_STARTS)
                : findMatchingStart(pitch, false, ZONE_STARTS, EDGE_STARTS, ALL_STARTS);
        if (start == null) {
            return null;
        }
        return new PitcherBotHardOption(kind, pitch, start, 0);
    }

    /**
     * 가중치 누적으로 HARD 옵션을 하나 고른다.
     *
     * <p>{@code r}을 {@code [0, sumWeights)}에서 뽑아 누적합이 넘는 첫 옵션을 선택한다.</p>
     *
     * @param options 가중 옵션(비어 있지 않아야 함)
     * @return 선택된 옵션
     * @throws IllegalArgumentException 옵션이 비었으면
     */
    PitcherBotHardOption sampleHardOption(List<PitcherBotHardOption> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("hard options are empty");
        }
        int sum = 0;
        for (PitcherBotHardOption option : options) {
            sum += option.weight();
        }
        if (sum <= 0) {
            return options.get(0);
        }
        int roll = random.nextInt(sum);
        return pickByCumulativeWeight(options, roll);
    }

    /**
     * 누적 가중치로 롤 값에 해당하는 옵션을 고른다(테스트용 분리).
     *
     * @param options 가중 옵션
     * @param roll    {@code [0, sumWeights)} 난수 값
     * @return 선택된 옵션
     */
    static PitcherBotHardOption pickByCumulativeWeight(List<PitcherBotHardOption> options, int roll) {
        int cumulative = 0;
        for (PitcherBotHardOption option : options) {
            cumulative += option.weight();
            if (roll < cumulative) {
                return option;
            }
        }
        return options.get(options.size() - 1);
    }

    /**
     * 패스트볼 여부.
     *
     * <p>{@code changeAmount == 0} 이거나 이름에 {@code 패스트볼}이 포함되면 true.</p>
     *
     * @param pitch 구종
     * @return 패스트볼이면 true
     */
    boolean isFastball(CardInfo pitch) {
        if (pitch.changeAmount() == 0) {
            return true;
        }
        String name = pitch.name();
        return name != null && name.contains("패스트볼");
    }

    /**
     * 유리 카운트용 변화구를 고른다.
     *
     * <p>비패스트볼 중 변화량이 가장 큰 것. 없으면 핸드 전체에서 동일 기준.</p>
     *
     * @param hand 핸드
     * @return 선택된 구종
     */
    CardInfo pickBreaking(List<CardInfo> hand) {
        List<CardInfo> breaking = hand.stream()
                .filter(p -> !isFastball(p))
                .toList();
        List<CardInfo> pool = breaking.isEmpty() ? hand : breaking;
        return pool.stream()
                .max(Comparator.comparingInt(CardInfo::changeAmount)
                        .thenComparing(CardInfo::cardId, Comparator.nullsLast(Long::compareTo)))
                .orElseThrow();
    }

    /**
     * 불리·동점 카운트용 패스트볼(또는 대체)을 고른다.
     *
     * @param hand       핸드
     * @param difficulty 난이도
     * @return 선택된 구종
     */
    CardInfo pickFastballOrSubstitute(List<CardInfo> hand, BotDifficulty difficulty) {
        List<CardInfo> fastballs = hand.stream()
                .filter(this::isFastball)
                .toList();
        if (!fastballs.isEmpty()) {
            // 변화량 0 우선(동점이면 cardId)
            return fastballs.stream()
                    .min(Comparator.comparingInt(CardInfo::changeAmount)
                            .thenComparing(CardInfo::cardId, Comparator.nullsLast(Long::compareTo)))
                    .orElseThrow();
        }
        return substituteMissingFastball(hand, difficulty);
    }

    /**
     * 패스트볼이 없을 때 대체 구종을 고른다.
     *
     * <p>EASY: DOWN 중 최소 변화량 → 없으면 최소 변화량 후 방향 DOWN&gt;SIDE&gt;REVERSE.
     * NORMAL/HARD: 최소 변화량 후 방향 DOWN&gt;SIDE&gt;REVERSE.</p>
     *
     * @param hand       핸드
     * @param difficulty 난이도
     * @return 대체 구종
     */
    CardInfo substituteMissingFastball(List<CardInfo> hand, BotDifficulty difficulty) {
        if (difficulty == BotDifficulty.EASY) {
            List<CardInfo> downs = hand.stream()
                    .filter(p -> p.direction() == ChangeDirection.DOWN)
                    .toList();
            if (!downs.isEmpty()) {
                return downs.stream()
                        .min(Comparator.comparingInt(CardInfo::changeAmount)
                                .thenComparing(CardInfo::cardId, Comparator.nullsLast(Long::compareTo)))
                        .orElseThrow();
            }
        }
        return hand.stream()
                .min(Comparator.comparingInt(CardInfo::changeAmount)
                        .thenComparingInt(p -> directionRank(p.direction()))
                        .thenComparing(CardInfo::cardId, Comparator.nullsLast(Long::compareTo)))
                .orElseThrow();
    }

    /**
     * 방향 우선순위(작을수록 우선): DOWN → SIDE → REVERSE.
     *
     * @param direction 변화 방향
     * @return 순위
     */
    private static int directionRank(ChangeDirection direction) {
        if (direction == null) {
            return Integer.MAX_VALUE;
        }
        return switch (direction) {
            case DOWN -> 0;
            case SIDE -> 1;
            case REVERSE -> 2;
        };
    }

    /**
     * 난이도·카운트에 맞는 시작 좌표를 고른다.
     *
     * @param pitch      구종
     * @param favorable  유리 카운트 여부
     * @param difficulty 난이도
     * @return 시작 좌표 1~25
     */
    private int pickStartCoordinate(CardInfo pitch, boolean favorable, BotDifficulty difficulty) {
        if (difficulty == BotDifficulty.EASY) {
            // EASY: 볼 금지 — 최종이 존 안
            return firstMatchingStartOrFallback(pitch, true, ZONE_STARTS, ALL_STARTS);
        }
        if (favorable) {
            // NORMAL 유리: 유인구(최종 존 밖) 우선, 없으면 존 안이라도
            Integer bait = findMatchingStart(pitch, false, ZONE_STARTS, EDGE_STARTS, ALL_STARTS);
            if (bait != null) {
                return bait;
            }
            return firstMatchingStartOrFallback(pitch, true, ZONE_STARTS, ALL_STARTS);
        }
        // NORMAL 불리: 존 승부
        return firstMatchingStartOrFallback(pitch, true, ZONE_STARTS, ALL_STARTS);
    }

    /**
     * 후보 시작 좌표 중 최종 존 여부가 {@code wantInZone}과 맞는 첫 좌표를 반환한다.
     *
     * @param pitch           구종
     * @param wantInZone      최종이 존 안이어야 하면 true
     * @param candidateGroups 우선순위별 후보 배열
     * @return 시작 좌표, 없으면 null
     */
    private Integer findMatchingStart(CardInfo pitch, boolean wantInZone, int[]... candidateGroups) {
        List<Integer> seen = new ArrayList<>();
        for (int[] group : candidateGroups) {
            for (int start : group) {
                if (seen.contains(start)) {
                    continue;
                }
                seen.add(start);
                int fin = PitchTrajectory.finalCoordinate(start, pitch);
                boolean inZone = PitchTrajectory.isStrikeZone(fin);
                if (wantInZone == inZone) {
                    return start;
                }
            }
        }
        return null;
    }

    /**
     * {@link #findMatchingStart} 결과가 없으면 중앙(13)으로 폴백한다.
     *
     * @param pitch           구종
     * @param wantInZone      최종이 존 안이어야 하면 true
     * @param candidateGroups 우선순위별 후보 배열
     * @return 시작 좌표
     */
    private int firstMatchingStartOrFallback(CardInfo pitch, boolean wantInZone, int[]... candidateGroups) {
        Integer found = findMatchingStart(pitch, wantInZone, candidateGroups);
        // best-effort: 의도 유지 실패 시 중앙
        return found != null ? found : 13;
    }
}
