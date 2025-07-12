package com.comdeply.comment.app.admin.cntr

import com.comdeply.comment.app.admin.svc.AdminManagementService
import com.comdeply.comment.app.admin.svc.AdminService
import com.comdeply.comment.app.admin.svc.vo.AdminActivationResponse
import com.comdeply.comment.app.admin.svc.vo.AdminDetailResponse
import com.comdeply.comment.app.admin.svc.vo.AdminStatisticsResponse
import com.comdeply.comment.app.admin.svc.vo.PasswordChangeResponse
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
@RequestMapping("/admin")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "관리자 계정 관리", description = "관리자 계정 관리 API")
class AdminController(
    private val adminService: AdminService,
    private val adminManagementService: AdminManagementService
) {
    private val logger = LoggerFactory.getLogger(AdminController::class.java)

    @GetMapping("/profile")
    @Operation(summary = "관리자 프로필 조회", description = "현재 로그인한 관리자의 프로필을 조회합니다")
    fun getProfile(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<AdminResponse>> {
        logger.info("관리자 프로필 조회 요청: adminId={}", userPrincipal.id)

        return try {
            val admin = adminManagementService.getProfile(userPrincipal.id)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = admin,
                    message = "관리자 프로필을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 프로필 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "관리자를 찾을 수 없습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 프로필 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("관리자 프로필 조회에 실패했습니다")
            )
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "관리자 프로필 수정", description = "현재 로그인한 관리자의 프로필을 수정합니다")
    fun updateProfile(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody
        request: AdminUpdateRequest
    ): ResponseEntity<ApiResponse<AdminResponse>> {
        logger.info("관리자 프로필 수정 요청: adminId={}", userPrincipal.id)

        return try {
            val updatedAdmin = adminManagementService.updateProfile(userPrincipal.id, request)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = updatedAdmin,
                    message = "관리자 프로필이 성공적으로 수정되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 프로필 수정 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "관리자 프로필 수정에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 프로필 수정 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("관리자 프로필 수정에 실패했습니다")
            )
        }
    }

    @PostMapping("/password")
    @Operation(summary = "관리자 비밀번호 변경", description = "현재 로그인한 관리자의 비밀번호를 변경합니다")
    fun changePassword(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody
        request: AdminPasswordChangeRequest
    ): ResponseEntity<ApiResponse<PasswordChangeResponse>> {
        logger.info("관리자 비밀번호 변경 요청: adminId={}", userPrincipal.id)

        return try {
            val response = adminManagementService.changePassword(userPrincipal.id, request)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = response,
                    message = "비밀번호 변경이 완료되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 비밀번호 변경 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "비밀번호 변경에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 비밀번호 변경 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("비밀번호 변경에 실패했습니다")
            )
        }
    }

    // === SUPER_ADMIN 전용 관리자 계정 관리 기능 ===

    @GetMapping("/admins")
    @Operation(summary = "관리자 목록 조회", description = "모든 관리자 목록을 조회합니다 (SUPER_ADMIN 전용)")
    fun getAdmins(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<out ApiResponse<out Any>?> {
        logger.info("관리자 목록 조회 요청: page={}, size={}", page, size)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val adminList = adminManagementService.getAdminList(currentAdmin, page, size)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = adminList,
                    message = "관리자 목록을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 목록 조회 실패: {}", e.message)
            ResponseEntity.status(403).body(
                ApiResponse.error(e.message ?: "관리자 목록 조회 권한이 없습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 목록 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("관리자 목록 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/admins/{adminId}")
    @Operation(summary = "관리자 상세 조회", description = "특정 관리자의 상세 정보를 조회합니다 (SUPER_ADMIN 전용)")
    fun getAdmin(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable adminId: Long
    ): ResponseEntity<ApiResponse<AdminDetailResponse>> {
        logger.info("관리자 상세 조회 요청: adminId={}", adminId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val adminDetail = adminManagementService.getAdminDetail(currentAdmin, adminId)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = adminDetail,
                    message = "관리자 정보를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 상세 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "관리자를 찾을 수 없습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 상세 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("관리자 상세 조회에 실패했습니다")
            )
        }
    }

    @PostMapping("/admins")
    @Operation(summary = "새 관리자 생성", description = "새로운 관리자를 생성합니다 (SUPER_ADMIN 전용)")
    fun createAdmin(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody
        request: AdminCreateRequest
    ): ResponseEntity<ApiResponse<AdminResponse>> {
        logger.info("새 관리자 생성 요청: username={}", request.username)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val newAdmin = adminManagementService.createAdmin(currentAdmin, request)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = newAdmin,
                    message = "새 관리자가 성공적으로 생성되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 생성 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "관리자 생성에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 생성 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("관리자 생성에 실패했습니다")
            )
        }
    }

    @PutMapping("/admins/{adminId}")
    @Operation(summary = "관리자 정보 수정", description = "관리자의 정보를 수정합니다 (SUPER_ADMIN 전용)")
    fun updateAdmin(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable adminId: Long,
        @Valid @RequestBody
        request: AdminUpdateRequest
    ): ResponseEntity<ApiResponse<AdminResponse>> {
        logger.info("관리자 정보 수정 요청: adminId={}", adminId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val updatedAdmin = adminManagementService.updateAdmin(currentAdmin, adminId, request)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = updatedAdmin,
                    message = "관리자 정보가 성공적으로 수정되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 정보 수정 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "관리자 정보 수정에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 정보 수정 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("관리자 정보 수정에 실패했습니다")
            )
        }
    }

    @PutMapping("/admins/{adminId}/role")
    @Operation(summary = "관리자 역할 변경", description = "관리자의 역할을 변경합니다 (SUPER_ADMIN 전용)")
    fun changeAdminRole(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable adminId: Long,
        @Valid @RequestBody
        request: AdminRoleChangeRequest
    ): ResponseEntity<ApiResponse<AdminResponse>> {
        logger.info("관리자 역할 변경 요청: adminId={}, newRole={}", adminId, request.role)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val updatedAdmin = adminManagementService.changeAdminRole(currentAdmin, adminId, request.role)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = updatedAdmin,
                    message = "관리자 역할이 성공적으로 변경되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 역할 변경 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "관리자 역할 변경에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 역할 변경 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("관리자 역할 변경에 실패했습니다")
            )
        }
    }

    @PostMapping("/admins/{adminId}/activate")
    @Operation(summary = "관리자 활성화", description = "관리자를 활성화합니다 (SUPER_ADMIN 전용)")
    fun activateAdmin(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable adminId: Long
    ): ResponseEntity<ApiResponse<AdminActivationResponse>> {
        logger.info("관리자 활성화 요청: adminId={}", adminId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val response = adminManagementService.activateAdmin(currentAdmin, adminId)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = response,
                    message = "관리자 활성화가 완료되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 활성화 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "관리자 활성화에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 활성화 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("관리자 활성화에 실패했습니다")
            )
        }
    }

    @PostMapping("/admins/{adminId}/deactivate")
    @Operation(summary = "관리자 비활성화", description = "관리자를 비활성화합니다 (SUPER_ADMIN 전용)")
    fun deactivateAdmin(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable adminId: Long
    ): ResponseEntity<ApiResponse<AdminActivationResponse>> {
        logger.info("관리자 비활성화 요청: adminId={}", adminId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val response = adminManagementService.deactivateAdmin(currentAdmin, adminId)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = response,
                    message = "관리자 비활성화가 완료되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 비활성화 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "관리자 비활성화에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 비활성화 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("관리자 비활성화에 실패했습니다")
            )
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "관리자 통계 조회", description = "관리자 관련 통계를 조회합니다 (SUPER_ADMIN 전용)")
    fun getAdminStats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<AdminStatisticsResponse>> {
        logger.info("관리자 통계 조회 요청")

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val stats = adminManagementService.getAdminStatistics(currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = stats,
                    message = "관리자 통계를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 통계 조회 실패: {}", e.message)
            ResponseEntity.status(403).body(
                ApiResponse.error(e.message ?: "관리자 통계 조회 권한이 없습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 통계 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("관리자 통계 조회에 실패했습니다")
            )
        }
    }
}
