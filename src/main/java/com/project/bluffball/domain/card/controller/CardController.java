package com.project.bluffball.domain.card.controller;

import com.project.bluffball.domain.card.dto.response.CoordinateCardResponse;
import com.project.bluffball.domain.card.entity.CoordinateCard;
import com.project.bluffball.domain.card.repository.CoordinateCardRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * 카드 데이터 조회 REST 컨트롤러.
 *
 * <p>인게임에서 사용하는 모든 카드 종류를 DB로부터 조회하는 API를 제공한다.
 * 카드 데이터는 정적 데이터에 가까우므로 게임 진입 시 클라이언트가 한 번 prefetch하여
 * 캐싱한 뒤, 이후 WebSocket 통신에서는 카드 ID만을 주고받는 구조로 설계되었다.</p>
 *
 * <p>카드 타입 구분 (JOINED 전략, {@code card} + 자식 테이블, {@code card_type_code} 기준):</p>
 * <ul>
 *   <li>{@code 0} — 기본 Card: 타이밍 카드({@code userType=BATTER}),
 *       구종 강화 카드({@code userType=PITCHER})</li>
 *   <li>{@code 1} — PitchCard: 구종 카드 (changeAmount, direction, timingCardId 포함)</li>
 *   <li>{@code 2} — CoordinateCard: 좌표 카드 (coordinateNumber 1~25, isStrike 포함)</li>
 * </ul>
 */
@Tag(name = "Card", description = "인게임 카드 데이터 조회 API")
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CoordinateCardRepository coordinateCardRepository;

    /**
     * 구종 카드 전체 목록 조회.
     *
     * <p>투수가 투구 시 선택하는 구종 카드({@code PitchCard}) 전체를 반환한다.
     * 각 카드에는 구종명, 좌표 변화량({@code changeAmount}), 변화 방향({@code direction}),
     * 연결된 타이밍 카드 ID({@code timingCardId})가 포함된다.</p>
     *
     * <p>변화구 최종 좌표 계산 공식:
     * {@code finalCoordinate = startCoordinate + (changeAmount × direction)}.
     * 격자(1~25) 이탈 시 최종 좌표는 0(폭투 존)으로 처리된다.</p>
     *
     * <p>클라이언트는 이 데이터를 prefetch하여 투수 투구 선택 화면(Phase 4)에서 사용한다.</p>
     */
    @Operation(
            summary = "구종 카드 목록 조회",
            description = "투수가 사용하는 구종 카드(PitchCard) 전체 목록을 반환한다. " +
                    "changeAmount와 direction으로 최종 좌표를 계산하며, " +
                    "timingCardId로 해당 구종의 타이밍 카드를 참조한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구종 카드 목록 반환 성공")
    })
    @GetMapping("/pitch")
    public ResponseEntity<List<?>> getPitchCards() {
        // TODO: CardService.getPitchCards()
        return ResponseEntity.ok().build();
    }

    /**
     * 좌표 카드 전체 목록 조회.
     *
     * <p>투수의 시작 좌표 선택 및 타자의 예측 좌표 선택에 사용되는
     * 좌표 카드({@code CoordinateCard}) 전체를 반환한다.
     * 각 카드에는 좌표 번호({@code coordinateNumber} 1~25)와
     * 스트라이크 존 여부({@code isStrike})가 포함된다.</p>
     *
     * <p>좌표 0(폭투 존) 카드는 {@code userType=BATTER} 전용이며 타자 UI에서만 사용한다.
     * 투수의 최종 좌표 0은 구종 변화 계산 결과로만 확정된다.</p>
     *
     * <p>클라이언트는 이 데이터를 prefetch하여
     * 투수 좌표 선택 화면(Phase 4) 및 타자 좌표 선택 화면(Phase 5)에서 사용한다.</p>
     */
    @Operation(
            summary = "좌표 카드 목록 조회",
            description = "투수 시작 좌표 선택 및 타자 예측 좌표 선택에 사용하는 " +
                    "좌표 카드(CoordinateCard) 전체 목록을 반환한다. " +
                    "coordinateNumber(1~25)와 isStrike(스트라이크 존 여부)가 포함된다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좌표 카드 목록 반환 성공")
    })
    @GetMapping("/coordinate")
    public ResponseEntity<List<CoordinateCardResponse>> getCoordinateCards() {
        List<CoordinateCardResponse> cards = coordinateCardRepository.findAll().stream()
                .sorted(Comparator.comparingInt(CoordinateCard::getCoordinateNumber))
                .map(card -> new CoordinateCardResponse(
                        card.getId(),
                        card.getCoordinateNumber(),
                        card.getName(),
                        card.isStrike()))
                .toList();
        return ResponseEntity.ok(cards);
    }

    /**
     * 구종 강화 카드 목록 조회.
     *
     * <p>투수가 구종 카드에 부착하여 효과를 강화하는 구종 강화 카드를 반환한다.
     * 기본 {@code Card} 타입({@code card_type_code=0})에서
     * {@code userType=PITCHER}인 카드들이다.</p>
     *
     * <p>강화 효과 종류:</p>
     * <ul>
     *   <li>구속 증가 — 타자 타이밍 판정 범위를 좁힌다</li>
     *   <li>변화폭 증가 — 구종 카드의 {@code changeAmount}를 증가시킨다</li>
     * </ul>
     */
    @Operation(
            summary = "구종 강화 카드 목록 조회",
            description = "투수가 구종 카드에 부착하여 구속 증가 또는 변화폭 증가 효과를 부여하는 " +
                    "강화 카드(userType=PITCHER) 목록을 반환한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구종 강화 카드 목록 반환 성공")
    })
    @GetMapping("/enhancement")
    public ResponseEntity<List<?>> getEnhancementCards() {
        // TODO: CardService.getEnhancementCards()
        return ResponseEntity.ok().build();
    }
}
