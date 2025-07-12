package com.comdeply.comment.app.admin.cntr

import com.comdeply.comment.app.admin.svc.AdminService
import com.comdeply.comment.app.admin.svc.AdminSiteService
import com.comdeply.comment.app.admin.svc.vo.SiteDeletionResponse
import com.comdeply.comment.app.admin.svc.vo.SiteStatsResponse
import com.comdeply.comment.app.admin.svc.vo.SiteStatusResponse
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
@RequestMapping("/admin/sites")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "관리자 - 사이트 관리", description = "관리자용 사이트 관리 API")
class AdminSiteController(
    private val adminService: AdminService,
    private val adminSiteService: AdminSiteService
) {
    private val logger = LoggerFactory.getLogger(AdminSiteController::class.java)

    @GetMapping
    @Operation(summary = "사이트 목록 조회", description = "관리자가 관리하는 사이트 목록을 조회합니다")
    fun getSites(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PageResponse<SiteResponse>>> {
        logger.info("관리자 사이트 목록 조회 요청: page={}, size={}", page, size)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val sitesPage = adminSiteService.getSitesByPermission(currentAdmin, page, size)
            val pageResponse = PageResponse.of(sitesPage)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = pageResponse,
                    message = "사이트 목록을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 목록 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "사이트 목록 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("사이트 목록 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("사이트 목록 조회에 실패했습니다")
            )
        }
    }

    @PostMapping
    @Operation(summary = "사이트 생성", description = "새로운 사이트를 생성합니다")
    fun createSite(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @Valid @RequestBody
        request: SiteCreateRequest
    ): ResponseEntity<ApiResponse<SiteResponse>> {
        logger.info("사이트 생성 요청: domain={}", request.domain)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val site = adminSiteService.createSite(request, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = site,
                    message = "사이트가 성공적으로 생성되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 생성 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "사이트 생성에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("사이트 생성 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("사이트 생성에 실패했습니다")
            )
        }
    }

    @GetMapping("/{siteId}")
    @Operation(summary = "사이트 상세 조회", description = "특정 사이트의 상세 정보를 조회합니다")
    fun getSiteDetail(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable siteId: Long
    ): ResponseEntity<ApiResponse<SiteResponse>> {
        logger.info("사이트 상세 조회 요청: siteId={}", siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val site = adminSiteService.getSiteDetail(siteId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = site,
                    message = "사이트 상세 정보를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 상세 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "사이트를 찾을 수 없습니다")
            )
        } catch (e: Exception) {
            logger.error("사이트 상세 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("사이트 상세 조회에 실패했습니다")
            )
        }
    }

    @PutMapping("/{siteId}")
    @Operation(summary = "사이트 수정", description = "사이트 정보를 수정합니다")
    fun updateSite(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable siteId: Long,
        @Valid @RequestBody
        request: SiteUpdateRequest
    ): ResponseEntity<ApiResponse<SiteResponse>> {
        logger.info("사이트 수정 요청: siteId={}", siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val updatedSite = adminSiteService.updateSite(siteId, request, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = updatedSite,
                    message = "사이트가 성공적으로 수정되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 수정 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "사이트 수정에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("사이트 수정 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("사이트 수정에 실패했습니다")
            )
        }
    }

    @DeleteMapping("/{siteId}")
    @Operation(summary = "사이트 삭제", description = "사이트를 삭제합니다 (비활성화)")
    fun deleteSite(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable siteId: Long
    ): ResponseEntity<ApiResponse<SiteDeletionResponse>> {
        logger.info("사이트 삭제 요청: siteId={}", siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val deletionResponse = adminSiteService.deleteSite(siteId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = deletionResponse,
                    message = "사이트 삭제가 완료되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 삭제 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "사이트 삭제에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("사이트 삭제 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("사이트 삭제에 실패했습니다")
            )
        }
    }

    @GetMapping("/{siteId}/stats")
    @Operation(summary = "사이트 통계 조회", description = "특정 사이트의 통계를 조회합니다")
    fun getSiteStats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable siteId: Long
    ): ResponseEntity<ApiResponse<SiteStatsResponse>> {
        logger.info("사이트 통계 조회 요청: siteId={}", siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val stats = adminSiteService.getSiteStats(siteId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = stats,
                    message = "사이트 통계를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 통계 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "사이트를 찾을 수 없습니다")
            )
        } catch (e: Exception) {
            logger.error("사이트 통계 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("사이트 통계 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/{siteId}/pages")
    @Operation(summary = "사이트별 페이지 목록 조회", description = "특정 사이트의 페이지 목록을 조회합니다")
    fun getSitePages(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable siteId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PageResponse<PageStatsResponse>>> {
        logger.info("사이트 페이지 목록 조회 요청: siteId={}, page={}, size={}", siteId, page, size)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val pagesPage = adminSiteService.getSitePages(siteId, currentAdmin, page, size)
            val pageResponse = PageResponse.of(pagesPage)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = pageResponse,
                    message = "사이트 페이지 목록을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 페이지 목록 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "사이트 페이지 목록 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("사이트 페이지 목록 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("사이트 페이지 목록 조회에 실패했습니다")
            )
        }
    }

    @PostMapping("/{siteId}/pages")
    @Operation(summary = "페이지 생성", description = "특정 사이트에 새로운 페이지를 생성합니다")
    fun createPage(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable siteId: Long,
        @Valid
        @RequestBody request: PageCreateRequest
    ): ResponseEntity<ApiResponse<SitePageResponse>> {
        logger.info("페이지 생성 요청: siteId={}, pageId={}", siteId, request.pageId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val page = adminSiteService.createPage(siteId, request, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = page,
                    message = "페이지가 성공적으로 생성되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("페이지 생성 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "페이지 생성에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("페이지 생성 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("페이지 생성에 실패했습니다")
            )
        }
    }

    @GetMapping("/{siteId}/pages/{pageId}")
    @Operation(summary = "페이지 상세 조회", description = "특정 페이지의 상세 정보를 조회합니다")
    fun getPageDetail(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable siteId: Long,
        @PathVariable pageId: String
    ): ResponseEntity<ApiResponse<SitePageResponse>> {
        logger.info("페이지 상세 조회 요청: siteId={}, pageId={}", siteId, pageId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val page = adminSiteService.getPageDetail(siteId, pageId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = page,
                    message = "페이지 상세 정보를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("페이지 상세 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "페이지를 찾을 수 없습니다")
            )
        } catch (e: Exception) {
            logger.error("페이지 상세 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("페이지 상세 조회에 실패했습니다")
            )
        }
    }

    @PutMapping("/{siteId}/pages/{pageId}")
    @Operation(summary = "페이지 수정", description = "페이지 정보를 수정합니다")
    fun updatePage(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable siteId: Long,
        @PathVariable pageId: String,
        @Valid
        @RequestBody request: com.comdeply.comment.dto.PageUpdateRequest
    ): ResponseEntity<ApiResponse<SitePageResponse>> {
        logger.info("페이지 수정 요청: siteId={}, pageId={}", siteId, pageId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val updatedPage = adminSiteService.updatePage(siteId, pageId, request, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = updatedPage,
                    message = "페이지가 성공적으로 수정되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("페이지 수정 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "페이지 수정에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("페이지 수정 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("페이지 수정에 실패했습니다")
            )
        }
    }

    @DeleteMapping("/{siteId}/pages/{pageId}")
    @Operation(summary = "페이지 삭제", description = "페이지를 삭제합니다 (비활성화)")
    fun deletePage(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable siteId: Long,
        @PathVariable pageId: String
    ): ResponseEntity<ApiResponse<Unit>> {
        logger.info("페이지 삭제 요청: siteId={}, pageId={}", siteId, pageId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            adminSiteService.deletePage(siteId, pageId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = Unit,
                    message = "페이지가 성공적으로 삭제되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("페이지 삭제 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "페이지 삭제에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("페이지 삭제 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("페이지 삭제에 실패했습니다")
            )
        }
    }

    @PutMapping("/{siteId}/status")
    @Operation(summary = "사이트 상태 변경", description = "사이트의 활성화/비활성화 상태를 변경합니다")
    fun toggleSiteStatus(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable siteId: Long
    ): ResponseEntity<ApiResponse<SiteStatusResponse>> {
        logger.info("사이트 상태 변경 요청: siteId={}", siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val statusResponse = adminSiteService.toggleSiteStatus(siteId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = statusResponse,
                    message = "사이트 상태가 성공적으로 변경되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 상태 변경 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "사이트 상태 변경에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("사이트 상태 변경 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("사이트 상태 변경에 실패했습니다")
            )
        }
    }
}
