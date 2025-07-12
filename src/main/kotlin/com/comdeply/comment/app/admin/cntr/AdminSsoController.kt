package com.comdeply.comment.app.admin.cntr

import com.comdeply.comment.app.admin.svc.*
import com.comdeply.comment.app.admin.svc.AdminService
import com.comdeply.comment.app.admin.svc.AdminSsoService
import com.comdeply.comment.app.admin.svc.vo.SsoAllSessionsTerminationResponse
import com.comdeply.comment.app.admin.svc.vo.SsoHmacKeyRegenerationResponse
import com.comdeply.comment.app.admin.svc.vo.SsoSessionTerminationResponse
import com.comdeply.comment.app.admin.svc.vo.SsoUserDeactivationResponse
import com.comdeply.comment.common.PageResponse
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.ApiResponse
import com.comdeply.comment.dto.SsoConfigRequest
import com.comdeply.comment.dto.SsoConfigResponse
import com.comdeply.comment.dto.SsoSessionResponse
import com.comdeply.comment.dto.SsoStatsResponse
import com.comdeply.comment.dto.SsoUserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/sso")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "관리자 SSO", description = "관리자용 SSO 관리 API")
class AdminSsoController(
    private val adminService: AdminService,
    private val adminSsoService: AdminSsoService
) {
    private val logger = LoggerFactory.getLogger(AdminSsoController::class.java)

    @GetMapping("/users")
    @Operation(summary = "SSO 사용자 목록 조회", description = "특정 사이트의 SSO 사용자 목록을 조회합니다")
    fun getSsoUsers(
        @RequestParam siteId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<PageResponse<SsoUserResponse>>> {
        logger.info("SSO 사용자 목록 조회: siteId={}, page={}, size={}", siteId, page, size)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val usersPage = adminSsoService.getSsoUsers(siteId, currentAdmin, page, size)
            val pageResponse = PageResponse.of(usersPage)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = pageResponse,
                    message = "SSO 사용자 목록을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("SSO 사용자 목록 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "SSO 사용자 목록 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("SSO 사용자 목록 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("SSO 사용자 목록 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/sessions")
    @Operation(summary = "SSO 세션 목록 조회", description = "특정 사이트의 SSO 세션 목록을 조회합니다")
    fun getSsoSessions(
        @RequestParam siteId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<PageResponse<SsoSessionResponse>>> {
        logger.info("SSO 세션 목록 조회: siteId={}, page={}, size={}", siteId, page, size)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val sessionsPage = adminSsoService.getSsoSessions(siteId, currentAdmin, page, size)
            val pageResponse = PageResponse.of(sessionsPage)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = pageResponse,
                    message = "SSO 세션 목록을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("SSO 세션 목록 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "SSO 세션 목록 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("SSO 세션 목록 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("SSO 세션 목록 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "SSO 통계 조회", description = "특정 사이트의 SSO 통계를 조회합니다")
    fun getSsoStats(
        @RequestParam siteId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<SsoStatsResponse>> {
        logger.info("SSO 통계 조회: siteId={}", siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val stats = adminSsoService.getSsoStats(siteId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = stats,
                    message = "SSO 통계를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("SSO 통계 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "SSO 통계 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("SSO 통계 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("SSO 통계 조회에 실패했습니다")
            )
        }
    }

    @PostMapping("/users/{ssoUserId}/deactivate")
    @Operation(summary = "SSO 사용자 비활성화", description = "특정 SSO 사용자를 비활성화합니다")
    fun deactivateSsoUserResponse(
        @PathVariable ssoUserId: Long,
        @RequestBody request: Map<String, Any>,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<SsoUserDeactivationResponse>> {
        logger.info("SSO 사용자 비활성화: ssoUserId={}", ssoUserId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val deactivationResponse = adminSsoService.deactivateSsoUser(ssoUserId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = deactivationResponse,
                    message = "SSO 사용자 비활성화가 완료되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("SSO 사용자 비활성화 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "SSO 사용자 비활성화에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("SSO 사용자 비활성화 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("SSO 사용자 비활성화에 실패했습니다")
            )
        }
    }

    @PostMapping("/sessions/{sessionId}/terminate")
    @Operation(summary = "SSO 세션 종료", description = "특정 SSO 세션을 종료합니다")
    fun terminateSession(
        @PathVariable sessionId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<SsoSessionTerminationResponse>> {
        logger.info("SSO 세션 종료: sessionId={}", sessionId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val terminationResponse = adminSsoService.terminateSession(sessionId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = terminationResponse,
                    message = "SSO 세션 종료가 완료되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("SSO 세션 종료 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "SSO 세션 종료에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("SSO 세션 종료 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("SSO 세션 종료에 실패했습니다")
            )
        }
    }

    @PostMapping("/sessions/terminate-all")
    @Operation(summary = "모든 SSO 세션 종료", description = "특정 사이트의 모든 SSO 세션을 종료합니다")
    fun terminateAllSessions(
        @RequestBody request: Map<String, Any>,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<SsoAllSessionsTerminationResponse>> {
        val siteId = (request["siteId"] as? Number)?.toLong() ?: throw IllegalArgumentException("siteId가 필요합니다")

        logger.info("모든 SSO 세션 종료: siteId={}", siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val terminationResponse = adminSsoService.terminateAllSessions(siteId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = terminationResponse,
                    message = "모든 SSO 세션 종료가 완료되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("모든 SSO 세션 종료 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "모든 SSO 세션 종료에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("모든 SSO 세션 종료 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("모든 SSO 세션 종료에 실패했습니다")
            )
        }
    }

    // === SSO Configuration Management (moved from SsoConfigController) ===

    @PostMapping("/config/{siteId}")
    @Operation(summary = "SSO 설정 생성/수정", description = "사이트의 SSO 설정을 생성하거나 수정합니다")
    fun updateSsoConfig(
        @Parameter(description = "사이트 ID", required = true)
        @PathVariable
        siteId: Long,
        @RequestBody request: SsoConfigRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<SsoConfigResponse>> {
        logger.info("SSO 설정 업데이트 요청: siteId={}, enabled={}", siteId, request.enabled)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val configResponse = adminSsoService.updateSsoConfig(siteId, request, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = configResponse,
                    message = "SSO 설정이 성공적으로 업데이트되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("SSO 설정 업데이트 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "SSO 설정 업데이트에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("SSO 설정 업데이트 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("SSO 설정 업데이트에 실패했습니다")
            )
        }
    }

    @GetMapping("/config/{siteId}")
    @Operation(summary = "SSO 설정 조회", description = "사이트의 SSO 설정을 조회합니다")
    fun getSsoConfig(
        @Parameter(description = "사이트 ID", required = true)
        @PathVariable
        siteId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<SsoConfigResponse>> {
        logger.info("SSO 설정 조회 요청: siteId={}", siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val configResponse = adminSsoService.getSsoConfig(siteId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = configResponse,
                    message = "SSO 설정을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("SSO 설정 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "SSO 설정 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("SSO 설정 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("SSO 설정 조회에 실패했습니다")
            )
        }
    }

    @PostMapping("/config/{siteId}/regenerate-key")
    @Operation(summary = "HMAC 키 재생성", description = "사이트의 HMAC 서명 키를 재생성합니다")
    fun regenerateHmacKey(
        @Parameter(description = "사이트 ID", required = true)
        @PathVariable
        siteId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<SsoHmacKeyRegenerationResponse>> {
        logger.info("HMAC 키 재생성 요청: siteId={}", siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            val regenerationResponse = adminSsoService.regenerateHmacKey(siteId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = regenerationResponse,
                    message = "HMAC 키가 재생성되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("HMAC 키 재생성 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "HMAC 키 재생성에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("HMAC 키 재생성 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("HMAC 키 재생성에 실패했습니다")
            )
        }
    }
}
