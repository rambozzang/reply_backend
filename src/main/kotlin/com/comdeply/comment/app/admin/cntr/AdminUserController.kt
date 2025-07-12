package com.comdeply.comment.app.admin.cntr

import com.comdeply.comment.app.admin.svc.AdminService
import com.comdeply.comment.app.admin.svc.AdminUserService
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/users")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "관리자 - 사용자 관리", description = "관리자용 사용자 관리 API")
class AdminUserController(
    private val adminService: AdminService,
    private val adminUserService: AdminUserService
) {
    private val logger = LoggerFactory.getLogger(AdminUserController::class.java)

    @GetMapping
    @Operation(summary = "일반 사용자 목록 조회", description = "권한에 따른 사용자 목록을 조회합니다")
    fun getUsers(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) siteId: Long?
    ): ResponseEntity<ApiResponse<UserListResponse>> {
        logger.info("사용자 목록 조회 요청: page={}, size={}, search={}, siteId={}", page, size, search, siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val userListResponse = adminUserService.getUsers(
                admin = currentAdmin,
                page = page,
                size = size,
                search = search,
                siteId = siteId
            )

            ResponseEntity.ok(
                ApiResponse.success(
                    data = userListResponse,
                    message = "사용자 목록을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사용자 목록 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "사용자 목록 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("사용자 목록 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("사용자 목록 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/{userId}")
    @Operation(summary = "일반 사용자 상세 조회", description = "특정 일반 사용자의 상세 정보를 조회합니다")
    fun getUserDetail(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable userId: Long
    ): ResponseEntity<ApiResponse<UserDetailResponse>> {
        logger.info("사용자 상세 조회 요청: userId={}", userId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val userDetail = adminUserService.getUserDetail(userId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = userDetail,
                    message = "사용자 상세 정보를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사용자 상세 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "사용자를 찾을 수 없습니다")
            )
        } catch (e: Exception) {
            logger.error("사용자 상세 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("사용자 상세 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/admins")
    @Operation(summary = "관리자 목록 조회", description = "모든 관리자 목록을 조회합니다")
    fun getAdmins(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<AdminListResponse>> {
        logger.info("관리자 목록 조회 요청: page={}, size={}", page, size)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val adminListResponse = adminUserService.getAdmins(currentAdmin, page, size)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = adminListResponse,
                    message = "관리자 목록을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 목록 조회 실패: {}", e.message)
            ResponseEntity.status(403).body(
                ApiResponse.error(e.message ?: "관리자 목록 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 목록 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("관리자 목록 조회에 실패했습니다")
            )
        }
    }
}
