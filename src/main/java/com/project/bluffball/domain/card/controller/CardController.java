package com.project.bluffball.domain.card.controller;

import com.project.bluffball.domain.card.dto.response.CoordinateCardResponse;
import com.project.bluffball.domain.card.dto.response.EnhancementCardResponse;
import com.project.bluffball.domain.card.dto.response.PitchCardResponse;
import com.project.bluffball.domain.card.entity.CoordinateCard;
import com.project.bluffball.domain.card.entity.PitchCard;
import com.project.bluffball.domain.card.repository.CoordinateCardRepository;
import com.project.bluffball.domain.card.repository.PitchCardRepository;
import com.project.bluffball.domain.card.service.EnhancementCardService;
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
 * <p>카드 타입 구분 (JOINED, {@code card_type_code}):</p>
 * <ul>
 *   <li>{@code 0} — 기본 Card</li>
 *   <li>{@code 1} — PitchCard</li>
 *   <li>{@code 2} — CoordinateCard</li>
 *   <li>{@code 3} — EnhancementCard</li>
 * </ul>
 */
@Tag(name = "Card", description = "인게임 카드 데이터 조회 API")
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CoordinateCardRepository coordinateCardRepository;
    private final PitchCardRepository pitchCardRepository;
    private final EnhancementCardService enhancementCardService;

    /**
     * 구종 카드 전체 목록을 조회한다.
     *
     * @return 구종 목록
     */
    @Operation(summary = "구종 카드 목록 조회")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "성공")})
    @GetMapping("/pitch")
    public ResponseEntity<List<PitchCardResponse>> getPitchCards() {
        List<PitchCardResponse> cards = pitchCardRepository.findAll().stream()
                .sorted(Comparator.comparing(PitchCard::getName))
                .map(PitchCardResponse::from)
                .toList();
        return ResponseEntity.ok(cards);
    }

    /**
     * 좌표 카드 전체 목록을 조회한다.
     *
     * @return 좌표 목록
     */
    @Operation(summary = "좌표 카드 목록 조회")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "성공")})
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
     * 강화 카드 마스터 목록을 조회한다.
     *
     * @return 강화 카드 3종
     */
    @Operation(
            summary = "구종 강화 카드 목록 조회",
            description = "변화량+1 / 타이밍 가속 / 타이밍 감속. 효과 분기는 effect enum."
    )
    @ApiResponses({@ApiResponse(responseCode = "200", description = "성공")})
    @GetMapping("/enhancement")
    public ResponseEntity<List<EnhancementCardResponse>> getEnhancementCards() {
        return ResponseEntity.ok(enhancementCardService.getCatalog());
    }
}
