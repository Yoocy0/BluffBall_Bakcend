package com.project.bluffball.domain.user.controller;

import com.project.bluffball.domain.user.dto.response.UserProfileResponse;
import com.project.bluffball.domain.user.dto.response.UserRecordsResponse;
import com.project.bluffball.domain.user.service.UserService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 유저 프로필·전적 REST 컨트롤러.
 *
 * <p>기본 프로필(닉네임·재화)과 전적(타자·투수 기록)을 분리해 제공한다.</p>
 */
@Tag(name = "User", description = "유저 프로필·전적 조회 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    /** 유저 조회 서비스 */
    private final UserService userService;

    /** JWT SecurityContext에서 userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * 내 기본 프로필을 조회한다.
     *
     * @return 닉네임·재화 등 기본 프로필
     */
    @Operation(
            summary = "내 프로필 조회",
            description = "JWT로 인증된 본인의 userId·닉네임·재화·닉네임 무료 변경 여부·튜토리얼 완료 여부·가입일을 반환한다. "
                    + "전적은 GET /api/v1/users/{userId}/records 로 별도 조회한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "유저 없음")
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userService.getMyProfile(userId));
    }

    /**
     * 지정 유저의 전적(타자·투수 기록)을 조회한다.
     *
     * @param userId 조회 대상 유저 ID
     * @return 전적 응답
     */
    @Operation(
            summary = "유저 전적 조회",
            description = "userId로 타자·투수 상세 성적(모드·리그별)을 조회한다. "
                    + "본인·타인 모두 조회 가능하다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전적 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음 또는 만료"),
            @ApiResponse(responseCode = "404", description = "유저 없음")
    })
    @GetMapping("/{userId}/records")
    public ResponseEntity<UserRecordsResponse> getRecords(
            @Parameter(description = "조회 대상 유저 ID")
            @PathVariable Long userId) {
        authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(userService.getRecords(userId));
    }
}
