package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.admin.svc.vo.SiteStatsResponse2
import com.comdeply.comment.app.web.svc.SiteService
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/sites")
@CrossOrigin(originPatterns = ["*"])
class SiteController(
    private val siteService: SiteService
) {

    // 인증 없이 접근 가능한 사이트 목록 (스킨 적용용)
    @GetMapping
    fun getAllSites(): ResponseEntity<SiteListResponse> {
        // 임시로 모든 활성 사이트 조회 (실제로는 공개 사이트만 조회)
        val sites = siteService.getActiveSites()
        return ResponseEntity.ok(sites)
    }

    // 최초 가입시 사이트 생성 엔드포인트
    @PostMapping("/initial")
    fun createInitialSite(
        @Valid @RequestBody
        request: SiteCreateRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal?
    ): ResponseEntity<SiteResponse> {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build()
        }
        val response = siteService.createInitialSite(request, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    // 온보딩용 임시 사이트 생성 엔드포인트 (인증 없음)
    @PostMapping("/onboarding")
    fun createSiteForOnboarding(
        @Valid @RequestBody
        request: SiteCreateRequest
    ): ResponseEntity<SiteResponse> {
        // 임시로 ownerId를 1로 고정 (실제로는 인증된 사용자 ID 사용)
        val response = siteService.createInitialSite(request, 1L)
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun createSite(
        @Valid @RequestBody
        request: SiteCreateRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<SiteResponse> {
        val response = siteService.createSite(request, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{siteId}")
    fun updateSite(
        @PathVariable siteId: Long,
        @Valid @RequestBody
        request: SiteUpdateRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<SiteResponse> {
        val response = siteService.updateSite(siteId, request, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{siteId}")
    fun getSite(@PathVariable siteId: Long): ResponseEntity<SiteResponse> {
        val response = siteService.getSite(siteId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/my")
    fun getUserSites(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<SiteListResponse> {
        val response = siteService.getUserSites(userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{siteId}")
    fun deleteSite(
        @PathVariable siteId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Void> {
        siteService.deleteSite(siteId, userPrincipal.id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{siteId}/stats")
    fun getSiteStats(
        @PathVariable siteId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<SiteStatsResponse2?> {
        val response = siteService.getSiteStats(siteId, userPrincipal.id)
        return ResponseEntity.ok(response)
    }
}
