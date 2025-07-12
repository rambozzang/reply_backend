package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.admin.svc.AdminService
import com.comdeply.comment.app.web.svc.SocialLoginService
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/social-login")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "소셜 로그인 관리", description = "소셜 로그인 설정 관리 API")
class SocialLoginController(
    private val socialLoginService: SocialLoginService,
    private val adminService: AdminService
) {
    private val logger = LoggerFactory.getLogger(SocialLoginController::class.java)

    @GetMapping("/settings")
    @Operation(summary = "소셜 로그인 설정 조회", description = "모든 소셜 로그인 제공자의 설정을 조회합니다")
    fun getSettings(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        logger.info("소셜 로그인 설정 조회 요청")

        try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val settings = socialLoginService.getSettings()

            return ResponseEntity.ok(
                ApiResponse.success(
                    data = settings,
                    message = "소셜 로그인 설정을 성공적으로 조회했습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("소셜 로그인 설정 조회 중 오류 발생", e)
            return ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "소셜 로그인 설정 조회에 실패했습니다: ${e.message}"
                )
            )
        }
    }

    @PutMapping("/settings")
    @Operation(summary = "소셜 로그인 설정 업데이트", description = "모든 소셜 로그인 제공자의 설정을 업데이트합니다")
    fun updateSettings(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody
        settings: Map<String, Any>
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        logger.info("소셜 로그인 설정 업데이트 요청")

        try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val updatedSettings = socialLoginService.updateSettings(settings)

            return ResponseEntity.ok(
                ApiResponse.success(
                    data = updatedSettings,
                    message = "소셜 로그인 설정이 성공적으로 업데이트되었습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("소셜 로그인 설정 업데이트 중 오류 발생", e)
            return ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "소셜 로그인 설정 업데이트에 실패했습니다: ${e.message}"
                )
            )
        }
    }

    @PutMapping("/settings/{provider}")
    @Operation(summary = "개별 제공자 설정 업데이트", description = "특정 소셜 로그인 제공자의 설정을 업데이트합니다")
    fun updateProvider(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable provider: String,
        @Valid @RequestBody
        settings: Map<String, Any>
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        logger.info("소셜 로그인 제공자 설정 업데이트 요청: provider={}", provider)

        try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val updatedSettings = socialLoginService.updateProvider(provider, settings)

            return ResponseEntity.ok(
                ApiResponse.success(
                    data = updatedSettings,
                    message = "$provider 설정이 성공적으로 업데이트되었습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("소셜 로그인 제공자 설정 업데이트 중 오류 발생", e)
            return ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "$provider 설정 업데이트에 실패했습니다: ${e.message}"
                )
            )
        }
    }

    @PostMapping("/test/{provider}")
    @Operation(summary = "설정 테스트", description = "특정 소셜 로그인 제공자의 설정을 테스트합니다")
    fun testProvider(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable provider: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        logger.info("소셜 로그인 제공자 설정 테스트 요청: provider={}", provider)

        try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val testResult = socialLoginService.testProvider(provider)

            return ResponseEntity.ok(
                ApiResponse.success(
                    data = testResult,
                    message = "$provider 설정 테스트가 완료되었습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("소셜 로그인 제공자 설정 테스트 중 오류 발생", e)
            return ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "$provider 설정 테스트에 실패했습니다: ${e.message}"
                )
            )
        }
    }

    @DeleteMapping("/settings")
    @Operation(summary = "설정 초기화", description = "모든 소셜 로그인 설정을 초기화합니다")
    fun resetSettings(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        logger.info("소셜 로그인 설정 초기화 요청")

        try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val defaultSettings = socialLoginService.resetSettings()

            return ResponseEntity.ok(
                ApiResponse.success(
                    data = defaultSettings,
                    message = "소셜 로그인 설정이 성공적으로 초기화되었습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("소셜 로그인 설정 초기화 중 오류 발생", e)
            return ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "소셜 로그인 설정 초기화에 실패했습니다: ${e.message}"
                )
            )
        }
    }
}
