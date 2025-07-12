package com.comdeply.comment.app.admin.cntr

import com.comdeply.comment.app.admin.svc.AdminSkinService
import com.comdeply.comment.dto.ApiResponse
import com.comdeply.comment.dto.SkinDto
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/skins")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "관리자 - 스킨 관리", description = "관리자용 사이트 관리 API")
class AdminSkinController(
    private val adminSkinService: AdminSkinService
) {
    
    /**
     * 관리자용 스킨 목록 조회
     * - 슈퍼 관리자: 모든 스킨 조회
     * - 일반 관리자: 공용 스킨 + 자신이 만든 스킨 조회
     */
    @GetMapping
    fun getSkins(
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<ApiResponse<List<SkinDto.SkinResponse>>> {
        val skins = adminSkinService.getSkins(token)
        return ResponseEntity.ok(ApiResponse.success(skins))
    }
    
    /**
     * 스킨 생성 (관리자용)
     */
    @PostMapping
    fun createSkin(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: SkinDto.CreateSkinRequest
    ): ResponseEntity<ApiResponse<SkinDto.SkinResponse>> {
        val skin = adminSkinService.createSkin(token, request)
        return ResponseEntity.ok(ApiResponse.success(skin))
    }
    
    /**
     * 스킨 수정 (관리자용)
     */
    @PutMapping("/{skinId}")
    fun updateSkin(
        @RequestHeader("Authorization") token: String,
        @PathVariable skinId: Long,
        @RequestBody request: SkinDto.UpdateSkinRequest
    ): ResponseEntity<ApiResponse<SkinDto.SkinResponse>> {
        val skin = adminSkinService.updateSkin(token, skinId, request)
        return ResponseEntity.ok(ApiResponse.success(skin))
    }
    
    /**
     * 스킨 삭제 (관리자용)
     */
    @DeleteMapping("/{skinId}")
    fun deleteSkin(
        @RequestHeader("Authorization") token: String,
        @PathVariable skinId: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        adminSkinService.deleteSkin(token, skinId)
        return ResponseEntity.ok(ApiResponse.success( Unit, "스킨이 성공적으로 삭제되었습니다."))
    }
    
    /**
     * 스킨 공유 토글 (관리자용)
     */
    @PostMapping("/{skinId}/toggle-sharing")
    fun toggleSkinSharing(
        @RequestHeader("Authorization") token: String,
        @PathVariable skinId: Long
    ): ResponseEntity<ApiResponse<SkinDto.SkinResponse>> {
        val skin = adminSkinService.toggleSkinSharing(token, skinId)
        return ResponseEntity.ok(ApiResponse.success(skin))
    }
    
    /**
     * 사이트별 스킨 적용 상황 조회
     */
    @GetMapping("/applications/{siteId}")
    fun getSkinApplications(
        @RequestHeader("Authorization") token: String,
        @PathVariable siteId: String
    ): ResponseEntity<ApiResponse<List<SkinDto.SkinApplyResponse>>> {
        val applications = adminSkinService.getSkinApplicationsBySite(token, siteId)
        return ResponseEntity.ok(ApiResponse.success(applications))
    }
    
    /**
     * 게시판별 스킨 적용
     */
    @PostMapping("/apply")
    fun applySkinToPage(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: SkinDto.SkinApplyRequest
    ): ResponseEntity<ApiResponse<SkinDto.SkinApplyResponse>> {
        val application = adminSkinService.applySkinToPage(token, request)
        return ResponseEntity.ok(ApiResponse.success(application, "스킨이 성공적으로 적용되었습니다."))
    }
    
    /**
     * 게시판별 스킨 적용 해제
     */
    @DeleteMapping("/applications/{applicationId}")
    fun removeSkinApplication(
        @RequestHeader("Authorization") token: String,
        @PathVariable applicationId: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        adminSkinService.removeSkinApplication(token, applicationId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "스킨 적용이 해제되었습니다."))
    }
    
    /**
     * 사이트의 모든 게시판에 스킨 일괄 적용
     */
    @PostMapping("/apply-to-all")
    fun applySkinToAllPages(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: SkinDto.SkinBulkApplyRequest
    ): ResponseEntity<ApiResponse<List<SkinDto.SkinApplyResponse>>> {
        val applications = adminSkinService.applySkinToAllPages(token, request)
        return ResponseEntity.ok(ApiResponse.success(applications, "모든 게시판에 스킨이 적용되었습니다."))
    }
    
    /**
     * 관리 가능한 사이트 목록 조회 (단순 목록)
     */
    @GetMapping("/sites")
    fun getManageableSites(
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<ApiResponse<List<String>>> {
        val sites = adminSkinService.getManageableSites(token)
        return ResponseEntity.ok(ApiResponse.success(sites))
    }
    
    /**
     * 사이트 관리 현황 전체 조회 (사이트, 페이지, 스킨 적용 상태 모두 포함)
     */
    @GetMapping("/management-overview")
    fun getSiteManagementOverview(
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<ApiResponse<SkinDto.SiteManagementResponse>> {
        val overview = adminSkinService.getSiteManagementOverview(token)
        return ResponseEntity.ok(ApiResponse.success(overview))
    }
    
    /**
     * 특정 사이트의 상세 정보 및 페이지별 스킨 적용 상태 조회
     */
    @GetMapping("/sites/{siteId}/details")
    fun getSiteDetailWithSkinStatus(
        @RequestHeader("Authorization") token: String,
        @PathVariable siteId: String
    ): ResponseEntity<ApiResponse<SkinDto.AdminSiteInfo>> {
        val siteInfo = adminSkinService.getSiteDetailWithSkinStatus(token, siteId)
        return ResponseEntity.ok(ApiResponse.success(siteInfo))
    }
    
    /**
     * 페이지 정보 등록/업데이트
     */
    @PostMapping("/sites/{siteId}/pages/{pageId}")
    fun registerOrUpdatePage(
        @RequestHeader("Authorization") token: String,
        @PathVariable siteId: String,
        @PathVariable pageId: String,
        @RequestBody request: PageUpdateRequest
    ): ResponseEntity<ApiResponse<SkinDto.SitePageInfo>> {
        val pageInfo = adminSkinService.registerOrUpdatePage(
            token, siteId, pageId, 
            request.pageName, request.pageDescription, request.pageType
        )
        return ResponseEntity.ok(ApiResponse.success(pageInfo, "페이지 정보가 업데이트되었습니다."))
    }
}

data class PageUpdateRequest(
    val pageName: String?,
    val pageDescription: String?,
    val pageType: com.comdeply.comment.entity.PageType = com.comdeply.comment.entity.PageType.BOARD
)