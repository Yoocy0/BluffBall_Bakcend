package com.project.bluffball.domain.team.controller;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.team.dto.request.UpsertMyPitchCardsRequest;
import com.project.bluffball.domain.team.dto.request.UpsertTeamLineupRequest;
import com.project.bluffball.domain.team.dto.request.UpsertTeamPitchCardsRequest;
import com.project.bluffball.domain.team.dto.response.TeamLineupResponse;
import com.project.bluffball.domain.team.dto.response.TeamPitchCardsResponse;
import com.project.bluffball.domain.team.service.TeamLineupService;
import com.project.bluffball.global.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 리그 출전 로스터·구종 사전 선택 REST 컨트롤러.
 *
 * <p>매칭 전 팀 단위로 Compact/Full 출전 명단과 구종·강화(n+1)를 구성한다.</p>
 */
@Tag(name = "Team Lineup", description = "리그 출전 로스터·구종 사전 선택 API")
@RestController
@RequestMapping("/api/v1/teams/{teamId}/lineups")
@RequiredArgsConstructor
public class TeamLineupController {

    /** 로스터·구종 사전 선택 서비스 */
    private final TeamLineupService teamLineupService;

    /** JWT SecurityContext에서 userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * 출전 로스터를 저장한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param request 출전 유저 목록
     * @return 저장된 로스터
     */
    @Operation(
            summary = "출전 로스터·선발 투수 저장",
            description = "Compact는 타순 3명 + 전담 투수 1명(startingPitcherUserId는 userIds 밖). "
                    + "Full은 타순 9명에 선발 투수가 포함된다. 리더만 가능하다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "인원 수·선발 투수 불일치 또는 팀원 아님"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "403", description = "리더가 아님")
    })
    @PutMapping("/{format}")
    public ResponseEntity<TeamLineupResponse> upsertLineup(
            @PathVariable Long teamId,
            @PathVariable LeagueFormat format,
            @Valid @RequestBody UpsertTeamLineupRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamLineupService.upsertLineup(userId, teamId, format, request));
    }

    /**
     * 출전 로스터를 조회한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 로스터
     */
    @Operation(
            summary = "출전 로스터·선발 투수 조회",
            description = "저장된 Compact/Full 타순과 선발 투수를 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "로스터 없음")
    })
    @GetMapping("/{format}")
    public ResponseEntity<TeamLineupResponse> getLineup(
            @PathVariable Long teamId,
            @PathVariable LeagueFormat format) {
        authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamLineupService.getLineup(teamId, format));
    }

    /**
     * 본인 구종 사전 선택을 저장한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param request 본인 카드·dropCard
     * @return 팀 전체 사전 선택 현황
     */
    @Operation(
            summary = "내 구종 사전 선택 저장",
            description = "출전 로스터에 포함된 멤버가 본인 보유 구종으로 사전 선택을 저장한다. "
                    + "카드 코스트 합 = Compact 4 / Full 5. dropCardId는 선택에 포함되어야 한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "코스트·보유·dropCard·로스터 오류"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "403", description = "팀 멤버가 아님")
    })
    @PutMapping("/{format}/pitch-cards/me")
    public ResponseEntity<TeamPitchCardsResponse> upsertMyPitchCards(
            @PathVariable Long teamId,
            @PathVariable LeagueFormat format,
            @Valid @RequestBody UpsertMyPitchCardsRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamLineupService.upsertMyPitchCards(userId, teamId, format, request));
    }

    /**
     * 멤버별 구종·강화 사전 선택을 저장한다. (레거시 리더 일괄)
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @param request 멤버별 카드 선택
     * @return 저장된 사전 선택
     * @deprecated {@code PUT .../pitch-cards/me} 사용
     */
    @Operation(
            summary = "구종 사전 선택 일괄 저장 (레거시)",
            description = "리더가 출전 멤버 전체를 일괄 저장한다. 개인 선택은 PUT .../pitch-cards/me 권장.",
            deprecated = true,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "코스트·보유·dropCard·로스터 불일치"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "403", description = "리더가 아님")
    })
    @Deprecated
    @PutMapping("/{format}/pitch-cards")
    public ResponseEntity<TeamPitchCardsResponse> upsertPitchCards(
            @PathVariable Long teamId,
            @PathVariable LeagueFormat format,
            @Valid @RequestBody UpsertTeamPitchCardsRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamLineupService.upsertPitchCards(userId, teamId, format, request));
    }

    /**
     * 멤버별 구종·강화 사전 선택을 조회한다.
     *
     * @param teamId 팀 ID
     * @param format 리그 구분
     * @return 사전 선택
     */
    @Operation(
            summary = "구종·강화 사전 선택 조회",
            description = "저장된 멤버별 구종·강화 사전 선택을 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "사전 선택 없음")
    })
    @GetMapping("/{format}/pitch-cards")
    public ResponseEntity<TeamPitchCardsResponse> getPitchCards(
            @PathVariable Long teamId,
            @PathVariable LeagueFormat format) {
        authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamLineupService.getPitchCards(teamId, format));
    }
}
