package com.project.bluffball.domain.tutorial.controller;

import com.project.bluffball.domain.tutorial.dto.request.TutorialCompleteRequest;
import com.project.bluffball.domain.tutorial.dto.response.TutorialCompleteResponse;
import com.project.bluffball.domain.tutorial.dto.response.TutorialStatusResponse;
import com.project.bluffball.domain.tutorial.service.TutorialService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 튜토리얼 REST 컨트롤러.
 *
 * <p>완료 여부 조회와 시작 구종 지급만 담당한다. 설명·퀴즈·데모는 프론트에서 처리한다.</p>
 */
@Tag(name = "Tutorial", description = "튜토리얼 완료 여부·시작 구종 지급 API")
@RestController
@RequestMapping("/api/v1/tutorial")
@RequiredArgsConstructor
public class TutorialController {

    /** 튜토리얼 서비스 */
    private final TutorialService tutorialService;

    /** JWT userId 추출 */
    private final AuthenticatedUserResolver authenticatedUserResolver;

    /**
     * 튜토리얼 완료 여부와 시작 구종 선택지를 조회한다.
     *
     * @return 완료 여부·포심/선택 구종 cardId
     */
    @Operation(
            summary = "튜토리얼 상태 조회",
            description = "완료 여부와 포심 고정 지급·커브/슬라이더/포크 선택지(cardId)를 반환한다. "
                    + "설명·퀴즈·데모 UI는 프론트에서 구성한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/status")
    public ResponseEntity<TutorialStatusResponse> getStatus() {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(tutorialService.getStatus(userId));
    }

    /**
     * 튜토리얼을 완료하고 시작 구종을 지급한다.
     *
     * @param request 선택 구종 2개
     * @return 지급된 구종
     */
    @Operation(
            summary = "튜토리얼 완료",
            description = "커브·슬라이더·포크 중 2개를 선택하면 포심과 함께 지급하고 튜토리얼을 완료 처리한다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "완료·지급 성공"),
            @ApiResponse(responseCode = "400", description = "선택 구종 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "409", description = "이미 완료")
    })
    @PostMapping("/complete")
    public ResponseEntity<TutorialCompleteResponse> complete(
            @Valid @RequestBody TutorialCompleteRequest request) {
        Long userId = authenticatedUserResolver.requireUserId();
        return ResponseEntity.ok(tutorialService.complete(userId, request));
    }
}
