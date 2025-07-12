package com.comdeply.comment.app.admin.cntr

import com.comdeply.comment.app.admin.svc.AdminAuthService
import com.comdeply.comment.app.admin.svc.vo.EmailCheckResponse
import com.comdeply.comment.app.admin.svc.vo.LogoutResponse
import com.comdeply.comment.app.admin.svc.vo.UsernameCheckResponse
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/auth")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "관리자 Auth", description = "관리자 인증 API")
class AdminAuthController(
    private val adminAuthService: AdminAuthService
) {
    private val logger = LoggerFactory.getLogger(AdminAuthController::class.java)

    @PostMapping("/login")
    @Operation(summary = "관리자 로그인", description = "관리자 계정으로 로그인합니다")
    fun login(
        @Valid @RequestBody
        request: AdminLoginRequest
    ): ResponseEntity<ApiResponse<AdminAuthResponse>> {
        logger.info("관리자 로그인 요청: {}", request.usernameOrEmail)

        return try {
            val response = adminAuthService.login(request)
            ResponseEntity.ok(
                ApiResponse.success(
                    data = response,
                    message = "로그인에 성공했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 로그인 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(
                    message = e.message ?: "로그인에 실패했습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("관리자 로그인 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "서버 오류가 발생했습니다"
                )
            )
        }
    }

    @PostMapping("/register")
    @Operation(summary = "관리자 회원가입", description = "새로운 관리자 계정을 생성합니다")
    fun register(
        @Valid @RequestBody
        request: AdminRegisterRequest
    ): ResponseEntity<ApiResponse<AdminRegisterResponse>> {
        logger.info("관리자 회원가입 요청: {}", request.username)

        return try {
            val response = adminAuthService.register(request)
            ResponseEntity.ok(
                ApiResponse.success(
                    data = response,
                    message = "회원가입에 성공했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 회원가입 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(
                    message = e.message ?: "회원가입에 실패했습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("관리자 회원가입 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "서버 오류가 발생했습니다"
                )
            )
        }
    }

    @GetMapping("/me")
    @Operation(summary = "현재 로그인한 관리자 정보 조회", description = "JWT 토큰을 통해 현재 로그인한 관리자 정보를 조회합니다")
    fun getCurrentAdmin(@AuthenticationPrincipal userPrincipal: UserPrincipal): ResponseEntity<ApiResponse<AdminResponse>> {
        logger.info("현재 관리자 정보 조회 요청")

        return try {
            val adminResponse = adminAuthService.getCurrentAdmin(userPrincipal.id)
            ResponseEntity.ok(
                ApiResponse.success(
                    data = adminResponse,
                    message = "관리자 정보를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("현재 관리자 정보 조회 실패: {}", e.message)
            ResponseEntity.status(401).body(
                ApiResponse.error(
                    message = e.message ?: "유효하지 않은 관리자입니다"
                )
            )
        } catch (e: Exception) {
            logger.error("현재 관리자 정보 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "서버 오류가 발생했습니다"
                )
            )
        }
    }

    @PostMapping("/check-username")
    @Operation(summary = "사용자명 중복 확인", description = "사용자명 중복 여부를 확인합니다")
    fun checkUsername(@RequestBody request: Map<String, String>): ResponseEntity<ApiResponse<UsernameCheckResponse>> {
        val username = request["username"] ?: return ResponseEntity.badRequest().body(
            ApiResponse.error(
                message = "사용자명이 필요합니다"
            )
        )

        return try {
            val response = adminAuthService.checkUsername(username)
            ResponseEntity.ok(
                ApiResponse.success(
                    data = response,
                    message = response.message
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사용자명 중복 확인 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(
                    message = e.message ?: "사용자명 확인에 실패했습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("사용자명 중복 확인 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "서버 오류가 발생했습니다"
                )
            )
        }
    }

    @PostMapping("/check-email")
    @Operation(summary = "이메일 중복 확인", description = "이메일 중복 여부를 확인합니다")
    fun checkEmail(@RequestBody request: Map<String, String>): ResponseEntity<ApiResponse<EmailCheckResponse>> {
        val email = request["email"] ?: return ResponseEntity.badRequest().body(
            ApiResponse.error(
                message = "이메일이 필요합니다"
            )
        )

        return try {
            val response = adminAuthService.checkEmail(email)
            ResponseEntity.ok(
                ApiResponse.success(
                    data = response,
                    message = response.message
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("이메일 중복 확인 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(
                    message = e.message ?: "이메일 확인에 실패했습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("이메일 중복 확인 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "서버 오류가 발생했습니다"
                )
            )
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "관리자 로그아웃", description = "관리자 로그아웃 처리 (클라이언트에서 토큰 제거)")
    fun logout(@AuthenticationPrincipal userPrincipal: UserPrincipal): ResponseEntity<ApiResponse<LogoutResponse>> {
        logger.info("관리자 로그아웃 요청")

        return try {
            val response = adminAuthService.logout(userPrincipal.id)
            ResponseEntity.ok(
                ApiResponse.success(
                    data = response,
                    message = response.message
                )
            )
        } catch (e: Exception) {
            logger.error("관리자 로그아웃 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "서버 오류가 발생했습니다"
                )
            )
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "관리자 프로필 업데이트", description = "현재 로그인한 관리자의 프로필을 업데이트합니다")
    fun updateProfile(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody
        request: AdminUpdateRequest
    ): ResponseEntity<ApiResponse<AdminResponse>> {
        logger.info("관리자 프로필 업데이트 요청")

        return try {
            val response = adminAuthService.updateProfile(userPrincipal.id, request)
            ResponseEntity.ok(
                ApiResponse.success(
                    data = response,
                    message = "프로필이 성공적으로 업데이트되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 프로필 업데이트 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(
                    message = e.message ?: "프로필 업데이트에 실패했습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("관리자 프로필 업데이트 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "서버 오류가 발생했습니다"
                )
            )
        }
    }

    @PutMapping("/password")
    @Operation(summary = "관리자 비밀번호 변경", description = "현재 로그인한 관리자의 비밀번호를 변경합니다")
    fun changePassword(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody
        request: AdminPasswordChangeRequest
    ): ResponseEntity<ApiResponse<AdminResponse>> {
        logger.info("관리자 비밀번호 변경 요청")

        return try {
            val response = adminAuthService.changePassword(userPrincipal.id, request)
            ResponseEntity.ok(
                ApiResponse.success(
                    data = response,
                    message = "비밀번호가 성공적으로 변경되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 비밀번호 변경 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(
                    message = e.message ?: "비밀번호 변경에 실패했습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("관리자 비밀번호 변경 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error(
                    message = "서버 오류가 발생했습니다"
                )
            )
        }
    }
}
