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
import com.project.bluffball.domain.league.service.LeagueService;
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
 * <p>리그 카탈로그·시즌·참가·순위·상금 조회 API를 제공한다.</p>
 */
@Tag(name = "League", description = "리그 API")
@RestController
@RequestMapping("/api/v1/leagues")
@RequiredArgsConstructor
public class LeagueController {

    /** 리그 유스케이스 조립 서비스 */
    private final LeagueService leagueService;

    /** JWT SecurityContext에서 userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * 리그 카탈로그 목록을 조회한다.
     *
     * @param format 리그 구분 필터
     * @param tier 리그 단계 필터
     * @return 리그 목록
     */
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
        return ResponseEntity.ok(leagueService.getLeagues(format, tier));
    }

    /**
     * 내 팀의 현재 소속 리그를 조회한다.
     *
     * @return 소속 리그 정보
     */
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
        return ResponseEntity.ok(leagueService.getMyLeague(userId));
    }

    /**
     * 시즌 목록을 조회한다.
     *
     * @param format 포맷 필터
     * @param tier 티어 필터
     * @param status 상태 필터
     * @return 시즌 목록
     */
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
        return ResponseEntity.ok(leagueService.getSeasons(format, tier, status));
    }

    /**
     * 시즌 상세를 조회한다.
     *
     * @param seasonId 시즌 ID
     * @return 시즌 상세
     */
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
        return ResponseEntity.ok(leagueService.getSeason(seasonId));
    }

    /**
     * 시즌 순위표를 조회한다.
     *
     * @param seasonId 시즌 ID
     * @return 순위 목록
     */
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
        return ResponseEntity.ok(leagueService.getStandings(seasonId));
    }

    /**
     * 시즌 상금표를 조회한다.
     *
     * @param seasonId 시즌 ID
     * @return 상금표
     */
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
        return ResponseEntity.ok(leagueService.getPrizes(seasonId));
    }

    /**
     * 시즌 참여권을 구매한다.
     *
     * @param seasonId 시즌 ID
     * @return 구매된 참여권
     */
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
        return ResponseEntity.ok(leagueService.purchaseTicket(userId, seasonId));
    }

    /**
     * 시즌에 참가한다.
     *
     * @param seasonId 시즌 ID
     * @return 참가 후 소속 정보
     */
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
        return ResponseEntity.ok(leagueService.joinSeason(userId, seasonId));
    }

    /**
     * 리그 등급 상세를 조회한다.
     *
     * @param leagueId 리그 ID
     * @return 리그 상세
     */
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
        return ResponseEntity.ok(leagueService.getLeague(leagueId));
    }
}
