package com.project.bluffball.domain.league.controller;

import com.project.bluffball.domain.league.dto.response.LeagueEntryTicketResponse;
import com.project.bluffball.domain.league.dto.response.LeaguePrizeResponse;
import com.project.bluffball.domain.league.dto.response.LeagueResponse;
import com.project.bluffball.domain.league.dto.response.LeagueSeasonResponse;
import com.project.bluffball.domain.league.dto.response.LeagueStandingItemResponse;
import com.project.bluffball.domain.league.dto.response.MyLeagueResponse;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueSeasonStatus;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.global.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 리그 REST 컨트롤러.
 *
 * <p>리그 카탈로그·시즌·참가·순위·상금 조회 API 껍데기를 제공한다.</p>
 */
@Tag(name = "League", description = "리그 API")
@RestController
@RequestMapping("/api/v1/leagues")
@RequiredArgsConstructor
public class LeagueController {

    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Operation(
            summary = "리그 카탈로그 조회",
            description = "Compact/Full × 7단계(총 14개) 리그 등급 목록을 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @GetMapping
    public ResponseEntity<List<LeagueResponse>> getLeagues(
            @Parameter(description = "리그 구분 필터")
            @RequestParam(required = false) LeagueFormat format,
            @Parameter(description = "리그 단계 필터")
            @RequestParam(required = false) LeagueTier tier) {
        authenticatedUserResolver.requireUserId();
        // TODO: return ResponseEntity.ok(leagueService.getLeagues(format, tier));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "내 팀 현재 리그 조회",
            description = "내 팀이 현재 소속된 리그 시즌 정보를 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "소속 리그 없음")
    })
    @GetMapping("/me")
    public ResponseEntity<MyLeagueResponse> getMyLeague() {
        Long userId = authenticatedUserResolver.requireUserId();
        // TODO: return ResponseEntity.ok(leagueService.getMyLeague(userId));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "리그 시즌 목록 조회",
            description = "포맷·티어·상태로 시즌을 조회한다. (예: 모집 중 시즌)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @GetMapping("/seasons")
    public ResponseEntity<List<LeagueSeasonResponse>> getSeasons(
            @RequestParam(required = false) LeagueFormat format,
            @RequestParam(required = false) LeagueTier tier,
            @RequestParam(required = false) LeagueSeasonStatus status) {
        authenticatedUserResolver.requireUserId();
        // TODO: return ResponseEntity.ok(leagueService.getSeasons(format, tier, status));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "리그 시즌 상세 조회",
            description = "시즌 식별값, 상태, 기간, 참가 팀 수 등을 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "시즌 없음")
    })
    @GetMapping("/seasons/{seasonId}")
    public ResponseEntity<LeagueSeasonResponse> getSeason(@PathVariable Long seasonId) {
        authenticatedUserResolver.requireUserId();
        // TODO: return ResponseEntity.ok(leagueService.getSeason(seasonId));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "리그 시즌 순위표",
            description = "점수 내림차순 참가 팀 순위 리스트를 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "시즌 없음")
    })
    @GetMapping("/seasons/{seasonId}/standings")
    public ResponseEntity<List<LeagueStandingItemResponse>> getStandings(@PathVariable Long seasonId) {
        authenticatedUserResolver.requireUserId();
        // TODO: return ResponseEntity.ok(leagueService.getStandings(seasonId));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "리그 시즌 상금표",
            description = "1등 상금과 순위별 퍼센트·상금액을 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "시즌 없음")
    })
    @GetMapping("/seasons/{seasonId}/prizes")
    public ResponseEntity<LeaguePrizeResponse> getPrizes(@PathVariable Long seasonId) {
        authenticatedUserResolver.requireUserId();
        // TODO: return ResponseEntity.ok(leagueService.getPrizes(seasonId));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "리그 참여권 구매",
            description = "팀 재정으로 해당 시즌 참여권을 구매한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "구매 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "400", description = "재정 부족 또는 잘못된 리그 단계"),
            @ApiResponse(responseCode = "409", description = "이미 참여권 보유")
    })
    @PostMapping("/seasons/{seasonId}/tickets")
    public ResponseEntity<LeagueEntryTicketResponse> purchaseTicket(@PathVariable Long seasonId) {
        Long userId = authenticatedUserResolver.requireUserId();
        // TODO: return ResponseEntity.ok(leagueService.purchaseTicket(userId, seasonId));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "리그 시즌 참가",
            description = "참여권 보유 및 최소 팀원 수를 검증한 뒤 시즌에 참가한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참가 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "400", description = "참여권 없음 또는 인원 부족"),
            @ApiResponse(responseCode = "409", description = "이미 참가 중 또는 시즌 정원 초과")
    })
    @PostMapping("/seasons/{seasonId}/join")
    public ResponseEntity<MyLeagueResponse> joinSeason(@PathVariable Long seasonId) {
        Long userId = authenticatedUserResolver.requireUserId();
        // TODO: return ResponseEntity.ok(leagueService.joinSeason(userId, seasonId));
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "리그 등급 상세 조회",
            description = "리그 식별값, 레벨, Compact/Full 구분, 참가비, 1등 상금 등을 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "리그 없음")
    })
    @GetMapping("/{leagueId}")
    public ResponseEntity<LeagueResponse> getLeague(@PathVariable Long leagueId) {
        authenticatedUserResolver.requireUserId();
        // TODO: return ResponseEntity.ok(leagueService.getLeague(leagueId));
        return ResponseEntity.ok().build();
    }
}
