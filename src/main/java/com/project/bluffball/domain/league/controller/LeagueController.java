package com.project.bluffball.domain.league.controller;

import com.project.bluffball.domain.league.dto.response.LeagueResponse;
import com.project.bluffball.domain.league.dto.response.TeamLeagueProgressResponse;
import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.league.service.LeagueProgressService;
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
 * <p>리그 카탈로그·상시 티어 진행(진입/승급) API를 제공한다.</p>
 */
@Tag(name = "League", description = "리그 API")
@RestController
@RequestMapping("/api/v1/leagues")
@RequiredArgsConstructor
public class LeagueController {

    /** 리그 카탈로그 서비스 */
    private final LeagueService leagueService;

    /** 상시 티어 진행 서비스 */
    private final LeagueProgressService leagueProgressService;

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
     * 내 팀의 상시 리그 진행 상태를 조회한다.
     *
     * @return 포맷별 진행 상태 목록
     */
    @Operation(
            summary = "내 팀 리그 진행 상태",
            description = "Compact/Full 각각 현재 티어·점수·승급 자격·전적을 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @GetMapping("/me")
    public ResponseEntity<List<TeamLeagueProgressResponse>> getMyProgress() {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(leagueProgressService.getMyProgress(userId));
    }

    /**
     * 포맷별 리그 진행 상태를 조회한다.
     *
     * @param format 포맷
     * @return 진행 상태
     */
    @Operation(
            summary = "포맷별 리그 진행 상태",
            description = "지정 포맷의 현재 티어·점수·승급 자격을 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "진행 상태 없음")
    })
    @GetMapping("/progress")
    public ResponseEntity<TeamLeagueProgressResponse> getProgress(
            @RequestParam LeagueFormat format) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(leagueProgressService.getProgress(userId, format));
    }

    /**
     * 최하위 티어로 최초 진입한다.
     *
     * @param format 포맷
     * @return 진입 후 진행 상태
     */
    @Operation(
            summary = "리그 최초 진입",
            description = "아마 4부부터 시작한다. 리더만 가능하며 참가비를 팀 금고에서 차감한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "진입 성공"),
            @ApiResponse(responseCode = "400", description = "재정 부족·인원 부족"),
            @ApiResponse(responseCode = "409", description = "이미 진입함")
    })
    @PostMapping("/enter")
    public ResponseEntity<TeamLeagueProgressResponse> enter(@RequestParam LeagueFormat format) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(leagueProgressService.enter(userId, format));
    }

    /**
     * 상위 티어로 승급한다.
     *
     * @param format 포맷
     * @param targetTier 목표 티어
     * @return 승급 후 진행 상태
     */
    @Operation(
            summary = "상위 리그 진출",
            description = "점수 상한 도달 후 상위 티어 참가비를 지불하고 승급한다. 리더만 가능.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승급 성공"),
            @ApiResponse(responseCode = "400", description = "자격 부족·재정 부족")
    })
    @PostMapping("/promote")
    public ResponseEntity<TeamLeagueProgressResponse> promote(
            @RequestParam LeagueFormat format,
            @RequestParam LeagueTier targetTier) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(leagueProgressService.promote(userId, format, targetTier));
    }

    /**
     * 리그 등급 상세를 조회한다.
     *
     * @param leagueId 리그 ID
     * @return 리그 상세
     */
    @Operation(
            summary = "리그 등급 상세 조회",
            description = "리그 식별값, Compact/Full 구분, 참가비 등을 반환한다.",
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
