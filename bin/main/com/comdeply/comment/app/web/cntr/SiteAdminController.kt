package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.admin.svc.AdminPermissionService
import com.comdeply.comment.app.admin.svc.AdminService
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.*
import com.comdeply.comment.repository.AdminRepository
import com.comdeply.comment.repository.AdminSiteRepository
import com.comdeply.comment.repository.SiteRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/site-admins")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "Site Admin Management", description = "사이트 관리자 관리 API")
class SiteAdminController(
    private val adminService: AdminService,
    private val adminPermissionService: AdminPermissionService,
    private val adminSiteRepository: AdminSiteRepository,
    private val adminRepository: AdminRepository,
    private val siteRepository: SiteRepository
) {
    private val logger = LoggerFactory.getLogger(SiteAdminController::class.java)

    @PostMapping("/assign")
    @Operation(summary = "사이트에 관리자 할당", description = "특정 사이트에 관리자를 할당합니다 (SUPER_ADMIN만 가능)")
    fun assignSiteAdmin(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestBody @Valid
        request: AssignSiteAdminRequest
    ): ResponseEntity<ApiResponse<SiteAdminResponse>> {
        logger.info("사이트 관리자 할당 요청: adminId={}, siteId={}", request.adminId, request.siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            // SUPER_ADMIN만 할당 가능
            if (!adminPermissionService.canAssignSiteAdmin(currentAdmin)) {
                return ResponseEntity.status(403).body(
                    ApiResponse.error("권한이 없습니다")
                )
            }

            // 대상 관리자 확인
            val targetAdmin = adminRepository.findById(request.adminId)
                .orElseThrow { IllegalArgumentException("존재하지 않는 관리자입니다") }

            // 사이트 확인
            val site = siteRepository.findById(request.siteId)
                .orElseThrow { IllegalArgumentException("존재하지 않는 사이트입니다") }

            // 할당 수행
            val adminSite = adminPermissionService.assignAdminToSite(
                assignerId = currentAdmin.id,
                adminId = request.adminId,
                siteId = request.siteId,
                permission = request.permission
            )

            val response = SiteAdminResponse(
                id = adminSite.id,
                adminId = adminSite.adminId,
                siteId = adminSite.siteId,
                permission = adminSite.permission,
                isActive = adminSite.isActive,
                createdAt = adminSite.createdAt,
                updatedAt = adminSite.updatedAt,
                assignedBy = adminSite.assignedBy,
                adminName = targetAdmin.name,
                adminEmail = targetAdmin.email,
                adminUsername = targetAdmin.username,
                siteName = site.siteName,
                siteDomain = site.domain
            )

            ResponseEntity.ok(ApiResponse.success(response, "사이트 관리자가 성공적으로 할당되었습니다"))
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 관리자 할당 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "할당에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("사이트 관리자 할당 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("시스템 오류가 발생했습니다")
            )
        }
    }

    @DeleteMapping("/unassign")
    @Operation(summary = "사이트에서 관리자 할당 해제", description = "특정 사이트에서 관리자 할당을 해제합니다")
    fun unassignSiteAdmin(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam adminId: Long,
        @RequestParam siteId: Long
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("사이트 관리자 할당 해제 요청: adminId={}, siteId={}", adminId, siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            // SUPER_ADMIN만 할당 해제 가능
            if (!adminPermissionService.canAssignSiteAdmin(currentAdmin)) {
                return ResponseEntity.status(403).body(
                    ApiResponse.error("권한이 없습니다")
                )
            }

            val success = adminPermissionService.unassignAdminFromSite(adminId, siteId)
            if (success) {
                ResponseEntity.ok(ApiResponse.success("사이트 관리자 할당이 해제되었습니다"))
            } else {
                ResponseEntity.badRequest().body(
                    ApiResponse.error("할당 해제에 실패했습니다")
                )
            }
        } catch (e: Exception) {
            logger.error("사이트 관리자 할당 해제 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("시스템 오류가 발생했습니다")
            )
        }
    }

    @GetMapping("/admin/{adminId}/sites")
    @Operation(summary = "관리자의 담당 사이트 목록 조회", description = "특정 관리자가 담당하는 사이트 목록을 조회합니다")
    fun getAdminSites(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Parameter(description = "관리자 ID") @PathVariable adminId: Long
    ): ResponseEntity<ApiResponse<AdminSitesResponse>> {
        logger.info("관리자 담당 사이트 목록 조회: adminId={}", adminId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            // 권한 확인: SUPER_ADMIN이거나 자기 자신의 정보만 조회 가능
            if (!adminPermissionService.canViewGlobalStats(currentAdmin) && currentAdmin.id != adminId) {
                return ResponseEntity.status(403).body(
                    ApiResponse.error("권한이 없습니다")
                )
            }

            val targetAdmin = adminRepository.findById(adminId)
                .orElseThrow { IllegalArgumentException("존재하지 않는 관리자입니다") }

            val adminSites = adminPermissionService.getAdminSitePermissions(adminId)
            val siteAdminResponses = adminSites.map { adminSite ->
                val site = siteRepository.findById(adminSite.siteId).orElse(null)
                SiteAdminResponse(
                    id = adminSite.id,
                    adminId = adminSite.adminId,
                    siteId = adminSite.siteId,
                    permission = adminSite.permission,
                    isActive = adminSite.isActive,
                    createdAt = adminSite.createdAt,
                    updatedAt = adminSite.updatedAt,
                    assignedBy = adminSite.assignedBy,
                    adminName = targetAdmin.name,
                    adminEmail = targetAdmin.email,
                    adminUsername = targetAdmin.username,
                    siteName = site?.siteName,
                    siteDomain = site?.domain ?: "Unknown"
                )
            }

            val response = AdminSitesResponse(
                adminId = adminId,
                adminName = targetAdmin.name,
                sites = siteAdminResponses
            )

            ResponseEntity.ok(ApiResponse.success(response, "관리자 담당 사이트 목록을 조회했습니다"))
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 담당 사이트 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("관리자 담당 사이트 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("시스템 오류가 발생했습니다")
            )
        }
    }

    @GetMapping("/site/{siteId}/admins")
    @Operation(summary = "사이트의 관리자 목록 조회", description = "특정 사이트에 할당된 관리자 목록을 조회합니다")
    fun getSiteAdmins(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Parameter(description = "사이트 ID") @PathVariable siteId: Long
    ): ResponseEntity<ApiResponse<SiteAdminsResponse>> {
        logger.info("사이트 관리자 목록 조회: siteId={}", siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            // 권한 확인: SUPER_ADMIN이거나 해당 사이트에 대한 권한이 있어야 함
            if (!adminPermissionService.canViewGlobalStats(currentAdmin) && !adminPermissionService.hasPermissionForSite(currentAdmin, siteId)) {
                return ResponseEntity.status(403).body(
                    ApiResponse.error("권한이 없습니다")
                )
            }

            val site = siteRepository.findById(siteId)
                .orElseThrow { IllegalArgumentException("존재하지 않는 사이트입니다") }

            val siteAdmins = adminPermissionService.getSiteAdmins(siteId)
            val adminResponses = siteAdmins.map { adminSite ->
                val admin = adminRepository.findById(adminSite.adminId).orElse(null)
                SiteAdminResponse(
                    id = adminSite.id,
                    adminId = adminSite.adminId,
                    siteId = adminSite.siteId,
                    permission = adminSite.permission,
                    isActive = adminSite.isActive,
                    createdAt = adminSite.createdAt,
                    updatedAt = adminSite.updatedAt,
                    assignedBy = adminSite.assignedBy,
                    adminName = admin?.name ?: "Unknown",
                    adminEmail = admin?.email ?: "Unknown",
                    adminUsername = admin?.username ?: "Unknown",
                    siteName = site.siteName,
                    siteDomain = site.domain
                )
            }

            val response = SiteAdminsResponse(
                siteId = siteId,
                siteName = site.siteName,
                siteDomain = site.domain,
                admins = adminResponses
            )

            ResponseEntity.ok(ApiResponse.success(response, "사이트 관리자 목록을 조회했습니다"))
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 관리자 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("사이트 관리자 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("시스템 오류가 발생했습니다")
            )
        }
    }

    @GetMapping("/assignments")
    @Operation(summary = "전체 사이트 관리자 할당 목록 조회", description = "모든 사이트 관리자 할당 정보를 조회합니다 (SUPER_ADMIN만 가능)")
    fun getAllAssignments(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<SiteAdminAssignmentsResponse>> {
        logger.info("전체 사이트 관리자 할당 목록 조회: page={}, size={}", page, size)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("유효하지 않은 관리자입니다")
                )

            // SUPER_ADMIN만 전체 목록 조회 가능
            if (!adminPermissionService.canViewGlobalStats(currentAdmin)) {
                return ResponseEntity.status(403).body(
                    ApiResponse.error("권한이 없습니다")
                )
            }

            val pageable = PageRequest.of(page, size)
            val adminSitePage = adminSiteRepository.findAll(pageable)

            val assignments = adminSitePage.content.map { adminSite ->
                val admin = adminRepository.findById(adminSite.adminId).orElse(null)
                val site = siteRepository.findById(adminSite.siteId).orElse(null)

                SiteAdminResponse(
                    id = adminSite.id,
                    adminId = adminSite.adminId,
                    siteId = adminSite.siteId,
                    permission = adminSite.permission,
                    isActive = adminSite.isActive,
                    createdAt = adminSite.createdAt,
                    updatedAt = adminSite.updatedAt,
                    assignedBy = adminSite.assignedBy,
                    adminName = admin?.name ?: "Unknown",
                    adminEmail = admin?.email ?: "Unknown",
                    adminUsername = admin?.username ?: "Unknown",
                    siteName = site?.siteName,
                    siteDomain = site?.domain ?: "Unknown"
                )
            }

            val response = SiteAdminAssignmentsResponse(
                assignments = assignments,
                totalCount = adminSitePage.totalElements
            )

            ResponseEntity.ok(ApiResponse.success(response, "사이트 관리자 할당 목록을 조회했습니다"))
        } catch (e: Exception) {
            logger.error("사이트 관리자 할당 목록 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("시스템 오류가 발생했습니다")
            )
        }
    }
}
