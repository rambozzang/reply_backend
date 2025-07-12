package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.SsoService
import com.comdeply.comment.dto.*
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.Base64

@RestController
@RequestMapping("/sso")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "SSO", description = "Single Sign-On API")
class SsoController(
    private val ssoService: SsoService
) {
    private val logger = LoggerFactory.getLogger(SsoController::class.java)

    @PostMapping("/auth")
    @Operation(summary = "SSO 인증", description = "고객 사이트에서 SSO 인증을 수행합니다")
    @ApiResponse(responseCode = "200", description = "인증 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    fun authenticate(
        @RequestBody request: SsoAuthRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<SsoAuthResponse> {
        val ipAddress = getClientIpAddress(httpRequest)
        val userAgent = httpRequest.getHeader("User-Agent")

        logger.info(
            "SSO 인증 요청: siteId={}, timestamp={}, ip={}",
            request.siteId,
            request.timestamp,
            ipAddress
        )

        val response = ssoService.authenticateUser(request, ipAddress, userAgent)

        return if (response.success) {
            logger.info(
                "SSO 인증 성공: siteId={}, userId={}",
                request.siteId,
                response.user?.id
            )
            ResponseEntity.ok(response)
        } else {
            logger.warn(
                "SSO 인증 실패: siteId={}, error={}",
                request.siteId,
                response.error
            )
            ResponseEntity.status(401).body(response)
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다")
    @ApiResponse(responseCode = "200", description = "토큰 갱신 성공")
    @ApiResponse(responseCode = "401", description = "토큰 갱신 실패")
    fun refreshToken(
        @RequestBody request: SsoRefreshRequest
    ): ResponseEntity<SsoAuthResponse> {
        logger.info("토큰 갱신 요청")

        val response = ssoService.refreshToken(request)

        return if (response.success) {
            logger.info("토큰 갱신 성공: userId={}", response.user?.id)
            ResponseEntity.ok(response)
        } else {
            logger.warn("토큰 갱신 실패: error={}", response.error)
            ResponseEntity.status(401).body(response)
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 세션을 종료합니다")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    fun logout(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Map<String, Any>> {
        val token = authorization.removePrefix("Bearer ")

        logger.info("로그아웃 요청")

        val success = ssoService.logout(token)

        return if (success) {
            logger.info("로그아웃 성공")
            ResponseEntity.ok(mapOf("success" to true, "message" to "로그아웃되었습니다"))
        } else {
            logger.warn("로그아웃 실패")
            ResponseEntity.status(400).body(mapOf("success" to false, "error" to "로그아웃에 실패했습니다"))
        }
    }

    @PostMapping("/logout-all")
    @Operation(summary = "전체 세션 로그아웃", description = "사용자의 모든 세션을 종료합니다")
    @ApiResponse(responseCode = "200", description = "전체 로그아웃 성공")
    fun logoutAll(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Map<String, Any>> {
        val token = authorization.removePrefix("Bearer ")

        logger.info("전체 세션 로그아웃 요청")

        // 토큰에서 사용자 정보 추출
        val userInfo = ssoService.validateToken(token)
        if (userInfo == null) {
            return ResponseEntity.status(401).body(mapOf("success" to false, "error" to "유효하지 않은 토큰입니다"))
        }

        val success = ssoService.logoutAllSessions(userInfo.id)

        return if (success) {
            logger.info("전체 세션 로그아웃 성공: userId={}", userInfo.id)
            ResponseEntity.ok(mapOf("success" to true, "message" to "모든 세션이 종료되었습니다"))
        } else {
            logger.warn("전체 세션 로그아웃 실패: userId={}", userInfo.id)
            ResponseEntity.status(400).body(mapOf("success" to false, "error" to "전체 로그아웃에 실패했습니다"))
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "토큰 검증", description = "액세스 토큰의 유효성을 검증합니다")
    @ApiResponse(responseCode = "200", description = "토큰 유효")
    @ApiResponse(responseCode = "401", description = "토큰 무효")
    fun validateToken(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Map<String, Any>> {
        val token = authorization.removePrefix("Bearer ")

        val userInfo = ssoService.validateToken(token)

        return if (userInfo != null) {
            ResponseEntity.ok(
                mapOf(
                    "valid" to true,
                    "user" to userInfo
                )
            )
        } else {
            ResponseEntity.status(401).body(
                mapOf(
                    "valid" to false,
                    "error" to "유효하지 않은 토큰입니다"
                )
            )
        }
    }

    @GetMapping("/users")
    @Operation(summary = "SSO 사용자 목록", description = "사이트별 SSO 사용자 목록을 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getSsoUsers(
        @Parameter(description = "사이트 ID", required = true)
        @RequestParam
        siteId: Long,

        @Parameter(description = "페이지 번호", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,

        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int
    ): ResponseEntity<SsoUserListResponse> {
        logger.info("SSO 사용자 목록 조회: siteId={}, page={}, size={}", siteId, page, size)

        val response = ssoService.getSsoUsers(siteId, page, size)

        return ResponseEntity.ok(response)
    }

    @GetMapping("/stats")
    @Operation(summary = "SSO 통계", description = "사이트별 SSO 사용 통계를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun getSsoStats(
        @Parameter(description = "사이트 ID", required = true)
        @RequestParam
        siteId: Long
    ): ResponseEntity<SsoStatsResponse> {
        logger.info("SSO 통계 조회: siteId={}", siteId)

        val response = ssoService.getSsoStats(siteId)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/cleanup")
    @Operation(summary = "만료된 세션 정리", description = "만료된 세션을 정리합니다 (관리자용)")
    @ApiResponse(responseCode = "200", description = "정리 완료")
    fun cleanupExpiredSessions(): ResponseEntity<Map<String, Any>> {
        logger.info("만료된 세션 정리 요청")

        val cleanedCount = ssoService.cleanupExpiredSessions()

        logger.info("만료된 세션 정리 완료: {} 개 세션 정리", cleanedCount)

        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "message" to "만료된 세션이 정리되었습니다",
                "cleanedCount" to cleanedCount
            )
        )
    }

    // === 테스트 및 디버깅용 API ===

    @GetMapping("/test/user-info")
    @Profile("!prod") // 프로덕션 환경에서는 비활성화
    @Operation(summary = "[테스트] 사용자 정보 조회", description = "토큰에서 사용자 정보를 추출합니다")
    fun testGetUserInfo(
        @RequestHeader("Authorization") authorization: String
    ): ResponseEntity<Map<String, Any>> {
        val token = authorization.removePrefix("Bearer ")

        val userInfo = ssoService.validateToken(token)

        return if (userInfo != null) {
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "userInfo" to userInfo
                )
            )
        } else {
            ResponseEntity.status(401).body(
                mapOf(
                    "success" to false,
                    "error" to "유효하지 않은 토큰입니다"
                )
            )
        }
    }

    @GetMapping("/test/create-sample-token")
    @Profile("!prod") // 프로덕션 환경에서는 비활성화
    @Operation(summary = "[테스트] 샘플 토큰 생성", description = "테스트용 샘플 토큰을 생성합니다")
    fun testCreateSampleToken(
        @RequestParam siteId: Long,
        @RequestParam userId: String,
        @RequestParam name: String,
        @RequestParam(required = false) email: String?
    ): ResponseEntity<Map<String, Any>> {
        try {
            val externalUserInfo = ExternalUserInfo(
                userId = userId,
                name = name,
                email = email
            )

            // Base64 인코딩된 사용자 정보 토큰 생성
            val userInfoJson = ObjectMapper().writeValueAsString(externalUserInfo)
            val userToken = Base64.getEncoder().encodeToString(userInfoJson.toByteArray())

            val timestamp = System.currentTimeMillis() / 1000

            return ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "sampleData" to mapOf(
                        "siteId" to siteId,
                        "userToken" to userToken,
                        "timestamp" to timestamp,
                        "userInfo" to externalUserInfo
                    ),
                    "note" to "실제 사용 시에는 HMAC 서명이 필요합니다"
                )
            )
        } catch (e: Exception) {
            logger.error("샘플 토큰 생성 중 오류 발생", e)
            return ResponseEntity.status(500).body(
                mapOf(
                    "success" to false,
                    "error" to "샘플 토큰 생성에 실패했습니다"
                )
            )
        }
    }

    // === Private Methods ===

    private fun getClientIpAddress(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIp = request.getHeader("X-Real-IP")

        return when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
            !xRealIp.isNullOrBlank() -> xRealIp
            else -> request.remoteAddr
        }
    }
}
