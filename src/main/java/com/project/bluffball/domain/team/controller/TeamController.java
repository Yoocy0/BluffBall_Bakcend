package com.project.bluffball.domain.team.controller;

import com.project.bluffball.domain.league.enums.LeagueFormat;
import com.project.bluffball.domain.league.enums.LeagueTier;
import com.project.bluffball.domain.team.dto.request.CreateTeamRequest;
import com.project.bluffball.domain.team.dto.request.DonateTeamRequest;
import com.project.bluffball.domain.team.dto.request.UpdateMemberRoleRequest;
import com.project.bluffball.domain.team.dto.request.UpdateTeamLogoRequest;
import com.project.bluffball.domain.team.dto.response.TeamMemberResponse;
import com.project.bluffball.domain.team.dto.response.TeamRecordItemResponse;
import com.project.bluffball.domain.team.dto.response.TeamRecordsResponse;
import com.project.bluffball.domain.team.dto.response.TeamResponse;
import com.project.bluffball.domain.team.dto.response.TeamTreasuryResponse;
import com.project.bluffball.domain.team.service.TeamService;
import com.project.bluffball.global.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 팀(클랜) REST 컨트롤러.
 *
 * <p>팀 창단·가입·탈퇴·삭제, 재화 기부, 멤버/기록 조회 등 팀 API를 제공한다.</p>
 */
@Tag(name = "Team", description = "팀(클랜) API")
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    /** 팀 유스케이스 조립 서비스 */
    private final TeamService teamService;

    /** JWT SecurityContext에서 userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * 팀을 창단한다.
     *
     * @param request 창단 요청
     * @return 생성된 팀 정보
     */
    @Operation(
            summary = "팀 창단",
            description = "팀을 생성한다. 팀 이름은 unique이며 이후 변경할 수 없다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "창단 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "409", description = "팀 이름 중복 또는 이미 소속 팀 존재")
    })
    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.create(userId, request));
    }

    /**
     * 내 소속 팀을 조회한다.
     *
     * @return 팀 정보
     */
    @Operation(
            summary = "내 팀 조회",
            description = "현재 로그인한 유저가 소속된 팀 정보를 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "소속 팀 없음")
    })
    @GetMapping("/me")
    public ResponseEntity<TeamResponse> getMyTeam() {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.getMyTeam(userId));
    }

    /**
     * 팀 이름으로 검색한다.
     *
     * @param name 검색어
     * @return 팀 목록
     */
    @Operation(
            summary = "팀 검색",
            description = "팀 이름으로 가입 대상 팀을 검색한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @GetMapping
    public ResponseEntity<List<TeamResponse>> search(
            @Parameter(description = "팀 이름 (부분 일치)")
            @RequestParam(required = false) String name) {
        authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.search(name));
    }

    /**
     * 팀 상세를 조회한다.
     *
     * @param teamId 팀 ID
     * @return 팀 정보
     */
    @Operation(
            summary = "팀 상세 조회",
            description = "팀 ID로 팀 상세 정보를 조회한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "팀 없음")
    })
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable Long teamId) {
        authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.getTeam(teamId));
    }

    /**
     * 팀을 삭제한다.
     *
     * @param teamId 팀 ID
     * @return 본문 없음
     */
    @Operation(
            summary = "팀 삭제",
            description = "팀을 삭제한다. 리더만 가능하다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "403", description = "리더가 아님"),
            @ApiResponse(responseCode = "404", description = "팀 없음")
    })
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> delete(@PathVariable Long teamId) {
        Long userId = authenticatedUserResolver.requireUserId();
        teamService.delete(userId, teamId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 팀에 가입한다.
     *
     * @param teamId 팀 ID
     * @return 가입한 팀 정보
     */
    @Operation(
            summary = "팀 가입",
            description = "팀에 가입한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가입 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "팀 없음"),
            @ApiResponse(responseCode = "409", description = "이미 다른 팀 소속")
    })
    @PostMapping("/{teamId}/join")
    public ResponseEntity<TeamResponse> join(@PathVariable Long teamId) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.join(userId, teamId));
    }

    /**
     * 팀에서 탈퇴한다.
     *
     * @param teamId 팀 ID
     * @return 본문 없음
     */
    @Operation(
            summary = "팀 탈퇴",
            description = "소속 팀에서 탈퇴한다. 리더는 위임 후 탈퇴해야 한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "409", description = "리더는 탈퇴 불가")
    })
    @PostMapping("/{teamId}/leave")
    public ResponseEntity<Void> leave(@PathVariable Long teamId) {
        Long userId = authenticatedUserResolver.requireUserId();
        teamService.leave(userId, teamId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 멤버를 강제 탈퇴시킨다.
     *
     * @param teamId 팀 ID
     * @param userId 대상 유저 ID
     * @return 본문 없음
     */
    @Operation(
            summary = "멤버 강제 탈퇴",
            description = "리더가 특정 멤버를 팀에서 내보낸다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "강제 탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "403", description = "리더가 아님")
    })
    @PostMapping("/{teamId}/members/{userId}/kick")
    public ResponseEntity<Void> kick(@PathVariable Long teamId, @PathVariable Long userId) {
        Long requesterId = authenticatedUserResolver.requireUserId();
        teamService.kick(requesterId, teamId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 팀 멤버 목록을 조회한다.
     *
     * @param teamId 팀 ID
     * @return 멤버 목록
     */
    @Operation(
            summary = "팀 멤버 목록",
            description = "팀 소속 유저 리스트와 계급, 온/오프라인 상태를 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "팀 없음")
    })
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getMembers(@PathVariable Long teamId) {
        authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.getMembers(teamId));
    }

    /**
     * 멤버 계급을 변경한다.
     *
     * @param teamId 팀 ID
     * @param userId 대상 유저 ID
     * @param request 계급 변경 요청
     * @return 변경된 멤버 정보
     */
    @Operation(
            summary = "멤버 계급 변경",
            description = "리더가 팀원 계급을 변경한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "403", description = "리더가 아님")
    })
    @PatchMapping("/{teamId}/members/{userId}/role")
    public ResponseEntity<TeamMemberResponse> updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        Long requesterId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.updateMemberRole(requesterId, teamId, userId, request));
    }

    /**
     * 팀 재정에 재화를 기부한다.
     *
     * @param teamId 팀 ID
     * @param request 기부 요청
     * @return 갱신된 재정 정보
     */
    @Operation(
            summary = "팀 재화 기부",
            description = "유저 재화를 팀 재정에 기부한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "기부 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "400", description = "재화 부족 또는 금액 오류")
    })
    @PostMapping("/{teamId}/donate")
    public ResponseEntity<TeamTreasuryResponse> donate(
            @PathVariable Long teamId,
            @Valid @RequestBody DonateTeamRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.donate(userId, teamId, request));
    }

    /**
     * 팀 재정을 조회한다.
     *
     * @param teamId 팀 ID
     * @return 재정 정보
     */
    @Operation(
            summary = "팀 재정 조회",
            description = "팀 재화 잔액과 거래 내역을 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "팀 없음")
    })
    @GetMapping("/{teamId}/treasury")
    public ResponseEntity<TeamTreasuryResponse> getTreasury(@PathVariable Long teamId) {
        authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.getTreasury(teamId));
    }

    /**
     * 팀 로고를 변경한다.
     *
     * @param teamId 팀 ID
     * @param request 로고 변경 요청
     * @return 갱신된 팀 정보
     */
    @Operation(
            summary = "팀 로고 변경",
            description = "팀 로고를 변경한다. 리더만 가능하다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "403", description = "리더가 아님")
    })
    @PatchMapping("/{teamId}/logo")
    public ResponseEntity<TeamResponse> updateLogo(
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamLogoRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.updateLogo(userId, teamId, request));
    }

    /**
     * 팀 리그 기록을 조회한다.
     *
     * @param teamId 팀 ID
     * @param seasonId 시즌 필터
     * @param format 포맷 필터
     * @param tier 티어 필터
     * @param aggregate 합산 여부
     * @return 기록 응답
     */
    @Operation(
            summary = "팀 리그 기록 조회",
            description = "시즌/포맷/티어 필터로 이력을 조회하거나, aggregate=true로 승·패·득실 합산을 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "팀 없음")
    })
    @GetMapping("/{teamId}/records")
    public ResponseEntity<TeamRecordsResponse> getRecords(
            @PathVariable Long teamId,
            @Parameter(description = "특정 시즌 ID")
            @RequestParam(required = false) Long seasonId,
            @Parameter(description = "리그 구분 (FULL / COMPACT)")
            @RequestParam(required = false) LeagueFormat format,
            @Parameter(description = "리그 단계 (AMATEUR_1 ~ PRO_2)")
            @RequestParam(required = false) LeagueTier tier,
            @Parameter(description = "true면 필터 범위 승/패/득실 합산")
            @RequestParam(required = false, defaultValue = "false") boolean aggregate) {
        authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.getRecords(teamId, seasonId, format, tier, aggregate));
    }

    /**
     * 특정 시즌 팀 기록을 조회한다.
     *
     * @param teamId 팀 ID
     * @param seasonId 시즌 ID
     * @return 시즌 기록
     */
    @Operation(
            summary = "특정 시즌 팀 기록 조회",
            description = "특정 시즌의 승/패/득실 기록을 반환한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "기록 없음")
    })
    @GetMapping("/{teamId}/records/{seasonId}")
    public ResponseEntity<TeamRecordItemResponse> getSeasonRecord(
            @PathVariable Long teamId,
            @PathVariable Long seasonId) {
        authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(teamService.getSeasonRecord(teamId, seasonId));
    }

    /**
     * 팀 멤버 프레즌스 하트비트를 전송한다.
     *
     * @param teamId 팀 ID
     * @return 본문 없음
     */
    @Operation(
            summary = "팀 멤버 프레즌스 하트비트",
            description = "팀 멤버 온/오프라인 유지를 위한 하트비트를 전송한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "하트비트 수신"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료")
    })
    @PostMapping("/{teamId}/presence/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable Long teamId) {
        Long userId = authenticatedUserResolver.requireUserId();
        teamService.heartbeat(userId, teamId);
        return ResponseEntity.noContent().build();
    }
}
