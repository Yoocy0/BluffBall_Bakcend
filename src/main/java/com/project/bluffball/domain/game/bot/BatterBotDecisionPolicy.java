package com.project.bluffball.domain.game.bot;

import com.project.bluffball.domain.game.dto.response.CardInfo;
import com.project.bluffball.domain.game.enums.BotDifficulty;
import com.project.bluffball.domain.game.enums.Timing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 타자 봇(EASY/NORMAL/HARD) 스윙 판단 정책.
 *
 * <ul>
 *   <li>소극 모드({@code seen.size() < 2}) — 선호 유형(HARD는 전체 풀)에 존·볼이 모두 있으면 지켜보기</li>
 *   <li>타자 유리({@code strikes ≤ balls}) — EASY/NORMAL: 패스트볼 노림</li>
 *   <li>타자 불리({@code strikes > balls}) — EASY/NORMAL: 변화구 노림</li>
 *   <li>EASY — 비소극 시 볼 무시·항상 스윙(1~25); 소극이어도 결과가 균일하면 스윙</li>
 *   <li>NORMAL — 예상 최종이 볼이면 좌표 0(지켜보기)</li>
 *   <li>HARD — 투수 HARD 우선순위·가중 샘플링을 투수 카운트 관점으로 재사용</li>
 * </ul>
 */
@Component
public class BatterBotDecisionPolicy {

    /** 패스트볼 분류·HARD 샘플링 재사용 */
    private final PitcherBotDecisionPolicy pitcherPolicy;

    /** 변화구 랜덤·HARD 샘플링용 난수 */
    private final Random random;

    /**
     * Spring 빈용 기본 생성자.
     *
     * @param pitcherPolicy 투수 정책(분류·대체·HARD 샘플링)
     */
    @Autowired
    public BatterBotDecisionPolicy(PitcherBotDecisionPolicy pitcherPolicy) {
        this(pitcherPolicy, new Random());
    }

    /**
     * 난수를 주입해 정책을 생성한다.
     *
     * @param pitcherPolicy 투수 정책
     * @param random        샘플링용 난수(null이면 새 Random)
     */
    BatterBotDecisionPolicy(PitcherBotDecisionPolicy pitcherPolicy, Random random) {
        this.pitcherPolicy = pitcherPolicy;
        this.random = random != null ? random : new Random();
    }

    /**
     * 시작 좌표·카운트·난이도·고려 풀로 스윙을 결정한다.
     *
     * <p>선택된 구종 ID를 읽지 않는다(PvP 공정성). {@code passiveMode}이면
     * 선호 유형(HARD는 전체 풀)에 존·볼이 모두 있을 때 지켜보기를 선호한다.</p>
     *
     * @param startCoordinateNumber PitcherReady 시작 좌표
     * @param balls                 볼 카운트
     * @param strikes               스트라이크 카운트
     * @param difficulty            EASY / NORMAL / HARD
     * @param considerationPool     메모리 규칙이 적용된 구종 풀
     * @param passiveMode           {@code seen.size() < 2} 소극 모드인지
     * @return 스윙/지켜보기 결정
     * @throws IllegalArgumentException 풀이 비었거나 난이도가 null이면
     */
    public BatterBotSwingDecision decide(
            int startCoordinateNumber,
            int balls,
            int strikes,
            BotDifficulty difficulty,
            List<CardInfo> considerationPool,
            boolean passiveMode) {
        if (considerationPool == null || considerationPool.isEmpty()) {
            throw new IllegalArgumentException("batter consideration pool is empty");
        }
        if (difficulty == null) {
            throw new IllegalArgumentException("difficulty is null");
        }

        return switch (difficulty) {
            case EASY -> decideEasy(
                    startCoordinateNumber, balls, strikes, considerationPool, passiveMode);
            case NORMAL -> decideNormal(
                    startCoordinateNumber, balls, strikes, considerationPool, passiveMode);
            case HARD -> decideHard(
                    startCoordinateNumber, balls, strikes, considerationPool, passiveMode);
        };
    }

    /**
     * EASY: 비소극 시 항상 스윙. 소극이면 선호 유형에 양존이 있으면 지켜보기.
     * 유리→패스트볼, 불리→존 안 변화구(랜덤).
     *
     * @param start        시작 좌표
     * @param balls        볼
     * @param strikes      스트라이크
     * @param pool         고려 풀
     * @param passiveMode  소극 모드
     * @return 스윙/지켜보기 결정
     */
    private BatterBotSwingDecision decideEasy(
            int start, int balls, int strikes, List<CardInfo> pool, boolean passiveMode) {
        boolean batterFavorable = isBatterFavorable(balls, strikes);
        List<CardInfo> preferred = preferredTypePool(pool, batterFavorable);

        if (passiveMode && hasBothZoneOutcomes(start, preferred)) {
            CardInfo assumed = preferred.isEmpty()
                    ? pool.get(0)
                    : pickRandom(preferred);
            return BatterBotSwingDecision.take(assumed != null ? assumed.name() : null);
        }

        CardInfo pitch = batterFavorable
                ? pitcherPolicy.pickFastballOrSubstitute(pool, BotDifficulty.NORMAL)
                : pickEasyBreaking(start, pool);
        return swingAtFinal(start, pitch);
    }

    /**
     * NORMAL: 예상 최종이 존이면 스윙, 볼이면 0.
     *
     * @param start        시작 좌표
     * @param balls        볼
     * @param strikes      스트라이크
     * @param pool         고려 풀
     * @param passiveMode  소극 모드
     * @return 결정
     */
    private BatterBotSwingDecision decideNormal(
            int start,
            int balls,
            int strikes,
            List<CardInfo> pool,
            boolean passiveMode) {
        boolean batterFavorable = isBatterFavorable(balls, strikes);
        List<CardInfo> preferred = preferredTypePool(pool, batterFavorable);

        if (passiveMode && hasBothZoneOutcomes(start, preferred)) {
            CardInfo assumed = preferred.isEmpty()
                    ? pool.get(0)
                    : pickRandom(preferred);
            return BatterBotSwingDecision.take(assumed != null ? assumed.name() : null);
        }

        CardInfo pitch = batterFavorable
                ? pitcherPolicy.pickFastballOrSubstitute(pool, BotDifficulty.NORMAL)
                : pickRandomBreakingOrFallback(pool);
        int fin = PitchTrajectory.finalCoordinate(start, pitch);
        if (PitchTrajectory.isStrikeZone(fin)) {
            return BatterBotSwingDecision.swing(fin, pitch.timing(), pitch.name());
        }
        return BatterBotSwingDecision.take(pitch.name());
    }

    /**
     * HARD: 투수 카운트 관점 우선순위 + 40/30/20/10 샘플링 후 타자 행동으로 변환.
     *
     * @param start        시작 좌표
     * @param balls        볼
     * @param strikes      스트라이크
     * @param pool         고려 풀
     * @param passiveMode  소극 모드
     * @return 결정
     */
    private BatterBotSwingDecision decideHard(
            int start,
            int balls,
            int strikes,
            List<CardInfo> pool,
            boolean passiveMode) {
        if (passiveMode && hasBothZoneOutcomes(start, pool)) {
            return BatterBotSwingDecision.take(pool.get(0).name());
        }

        boolean batterFavorable = isBatterFavorable(balls, strikes);
        List<PitcherBotHardOption> options = planHardBatterOptions(start, pool, balls, strikes, batterFavorable);
        if (options.isEmpty()) {
            // 실현 옵션 없음 — 중앙 스윙 폴백
            CardInfo fallback = pool.get(0);
            return swingAtFinal(start, fallback);
        }

        PitcherBotHardOption picked = pitcherPolicy.sampleHardOption(options);
        return mapHardOptionToAction(start, picked);
    }

    /**
     * HARD용 실현 가능 옵션을 투수 우선순위로 만든다(시작 좌표 고정).
     *
     * @param start            알려진 시작 좌표
     * @param pool             고려 풀
     * @param balls            볼
     * @param strikes          스트라이크
     * @param batterFavorable  타자 유리 여부(변화구 존 필터용)
     * @return 가중 옵션
     */
    List<PitcherBotHardOption> planHardBatterOptions(
            int start,
            List<CardInfo> pool,
            int balls,
            int strikes,
            boolean batterFavorable) {
        // 투수가 유리한 카운트면 투수 FAVORABLE 표
        boolean pitcherFavorable = strikes > balls;
        List<PitcherBotOptionKind> priority = pitcherFavorable
                ? PitcherBotDecisionPolicy.HARD_FAVORABLE_PRIORITY
                : PitcherBotDecisionPolicy.HARD_UNFAVORABLE_PRIORITY;

        List<PitcherBotHardOption> realizable = new ArrayList<>();
        for (PitcherBotOptionKind kind : priority) {
            PitcherBotHardOption option = tryRealizeHardBatterOption(start, pool, kind, batterFavorable);
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
                    PitcherBotDecisionPolicy.HARD_WEIGHTS[i]));
        }
        return weighted;
    }

    /**
     * HARD 옵션을 고정 시작 좌표·고려 풀에서 실현한다.
     *
     * @param start           시작 좌표
     * @param pool            고려 풀
     * @param kind            옵션 종류
     * @param batterFavorable 타자 유리(변화구 양존 시 필터)
     * @return 가중치 0 후보, 불가면 null
     */
    private PitcherBotHardOption tryRealizeHardBatterOption(
            int start,
            List<CardInfo> pool,
            PitcherBotOptionKind kind,
            boolean batterFavorable) {
        boolean wantFastball = kind == PitcherBotOptionKind.FASTBALL_STRIKE
                || kind == PitcherBotOptionKind.FASTBALL_BALL;
        boolean wantInZone = kind == PitcherBotOptionKind.FASTBALL_STRIKE
                || kind == PitcherBotOptionKind.BREAKING_STRIKE;

        CardInfo pitch;
        if (wantFastball) {
            pitch = pitcherPolicy.pickFastballOrSubstitute(pool, BotDifficulty.NORMAL);
            int fin = PitchTrajectory.finalCoordinate(start, pitch);
            if (PitchTrajectory.isStrikeZone(fin) != wantInZone) {
                return null;
            }
        } else {
            pitch = pickHardBreaking(start, pool, wantInZone, batterFavorable);
            if (pitch == null) {
                return null;
            }
        }
        return new PitcherBotHardOption(kind, pitch, start, 0);
    }

    /**
     * HARD 변화구 후보를 고른다.
     *
     * <p>시작 기준 존 안/밖 변화구가 모두 있으면 타자 유리→볼(밖), 불리→스트라이크(안)만 남긴다.
     * 그다음 {@code wantInZone}과 맞는 후보에서 랜덤. 한 클래스만 있으면 그 클래스만 사용.</p>
     *
     * @param start           시작 좌표
     * @param pool            고려 풀
     * @param wantInZone      옵션이 요구하는 존 여부
     * @param batterFavorable 타자 유리
     * @return 선택된 변화구, 없으면 null
     */
    private CardInfo pickHardBreaking(
            int start,
            List<CardInfo> pool,
            boolean wantInZone,
            boolean batterFavorable) {
        List<CardInfo> breaking = breakingPitches(pool);
        if (breaking.isEmpty()) {
            return null;
        }

        List<CardInfo> inZone = new ArrayList<>();
        List<CardInfo> outZone = new ArrayList<>();
        for (CardInfo pitch : breaking) {
            if (PitchTrajectory.isStrikeZone(PitchTrajectory.finalCoordinate(start, pitch))) {
                inZone.add(pitch);
            } else {
                outZone.add(pitch);
            }
        }

        List<CardInfo> candidates;
        if (!inZone.isEmpty() && !outZone.isEmpty()) {
            // 양존 공존 시 카운트로 의도 고정 — 옵션 wantInZone과 다르면 실현 불가
            boolean forceInZone = !batterFavorable;
            if (forceInZone != wantInZone) {
                return null;
            }
            candidates = forceInZone ? inZone : outZone;
        } else if (!inZone.isEmpty()) {
            if (!wantInZone) {
                return null;
            }
            candidates = inZone;
        } else {
            if (wantInZone) {
                return null;
            }
            candidates = outZone;
        }
        return pickRandom(candidates);
    }

    /**
     * HARD 옵션을 타자 행동으로 변환한다.
     *
     * @param start  시작 좌표
     * @param option 샘플된 옵션
     * @return 스윙/지켜보기
     */
    private BatterBotSwingDecision mapHardOptionToAction(int start, PitcherBotHardOption option) {
        CardInfo pitch = option.pitch();
        int fin = PitchTrajectory.finalCoordinate(start, pitch);
        boolean ballIntent = option.kind() == PitcherBotOptionKind.FASTBALL_BALL
                || option.kind() == PitcherBotOptionKind.BREAKING_BALL;

        if (ballIntent) {
            // 볼 의도: 최종이 존 밖이면 미끼 스윙, 존 안이면 지켜보기
            if (PitchTrajectory.isStrikeZone(fin)) {
                return BatterBotSwingDecision.take(pitch.name());
            }
            return BatterBotSwingDecision.swing(fin, pitch.timing(), pitch.name());
        }
        return BatterBotSwingDecision.swing(fin, pitch.timing(), pitch.name());
    }

    /**
     * EASY 변화구: 최종 존 안인 것 중 랜덤 → 없으면 아무 변화구 → 없으면 아무 구종.
     *
     * @param start 시작 좌표
     * @param pool  고려 풀
     * @return 구종
     */
    private CardInfo pickEasyBreaking(int start, List<CardInfo> pool) {
        List<CardInfo> breaking = breakingPitches(pool);
        List<CardInfo> inZone = breaking.stream()
                .filter(p -> PitchTrajectory.isStrikeZone(PitchTrajectory.finalCoordinate(start, p)))
                .toList();
        if (!inZone.isEmpty()) {
            return pickRandom(inZone);
        }
        if (!breaking.isEmpty()) {
            return pickRandom(breaking);
        }
        return pickRandom(pool);
    }

    /**
     * NORMAL/폴백용 랜덤 변화구. 없으면 풀에서 랜덤.
     *
     * @param pool 고려 풀
     * @return 구종
     */
    private CardInfo pickRandomBreakingOrFallback(List<CardInfo> pool) {
        List<CardInfo> breaking = breakingPitches(pool);
        if (!breaking.isEmpty()) {
            return pickRandom(breaking);
        }
        return pickRandom(pool);
    }

    /**
     * 선호 유형(패스트볼/변화구) 부분 풀.
     *
     * @param pool             전체 고려 풀
     * @param batterFavorable  타자 유리면 패스트볼
     * @return 선호 유형 목록(비어 있을 수 있음)
     */
    private List<CardInfo> preferredTypePool(List<CardInfo> pool, boolean batterFavorable) {
        if (batterFavorable) {
            List<CardInfo> fastballs = pool.stream()
                    .filter(pitcherPolicy::isFastball)
                    .toList();
            return fastballs.isEmpty() ? pool : fastballs;
        }
        List<CardInfo> breaking = breakingPitches(pool);
        return breaking.isEmpty() ? pool : breaking;
    }

    /**
     * 비패스트볼 목록.
     *
     * @param pool 고려 풀
     * @return 변화구
     */
    private List<CardInfo> breakingPitches(List<CardInfo> pool) {
        return pool.stream()
                .filter(p -> !pitcherPolicy.isFastball(p))
                .toList();
    }

    /**
     * 후보들의 최종 좌표에 존 안·밖이 모두 있는지.
     *
     * @param start 시작 좌표
     * @param pool  후보
     * @return 양존이 모두 있으면 true
     */
    private static boolean hasBothZoneOutcomes(int start, List<CardInfo> pool) {
        if (pool == null || pool.isEmpty()) {
            return false;
        }
        boolean sawIn = false;
        boolean sawOut = false;
        for (CardInfo pitch : pool) {
            if (PitchTrajectory.isStrikeZone(PitchTrajectory.finalCoordinate(start, pitch))) {
                sawIn = true;
            } else {
                sawOut = true;
            }
            if (sawIn && sawOut) {
                return true;
            }
        }
        return false;
    }

    /**
     * 타자 유리 카운트: {@code strikes ≤ balls} (투수 불리·동점의 뒤집기).
     *
     * @param balls   볼
     * @param strikes 스트라이크
     * @return 타자 유리면 true
     */
    static boolean isBatterFavorable(int balls, int strikes) {
        return strikes <= balls;
    }

    /**
     * 최종 좌표로 스윙 결정을 만든다.
     *
     * @param start 시작 좌표
     * @param pitch 가정 구종
     * @return 스윙
     */
    private static BatterBotSwingDecision swingAtFinal(int start, CardInfo pitch) {
        int fin = PitchTrajectory.finalCoordinate(start, pitch);
        // EASY는 볼 금지 의도이나 폴백 시 최종 0/존밖일 수 있음 — 그래도 스윙(1~25 클램프)
        int coord = fin >= 1 && fin <= 25 ? fin : 13;
        Timing timing = pitch.timing() != null ? pitch.timing() : Timing.NORMAL;
        return BatterBotSwingDecision.swing(coord, timing, pitch.name());
    }

    /**
     * 목록에서 균등 랜덤 하나를 고른다.
     *
     * @param list 비어 있지 않은 목록
     * @return 선택 요소
     */
    private CardInfo pickRandom(List<CardInfo> list) {
        return list.get(random.nextInt(list.size()));
    }
}
