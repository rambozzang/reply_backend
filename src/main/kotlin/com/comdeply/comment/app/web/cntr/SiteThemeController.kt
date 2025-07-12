package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.SiteThemeService
import com.comdeply.comment.dto.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/site-themes")
class SiteThemeController(
    private val siteThemeService: SiteThemeService
) {

    @PostMapping
    fun applyThemeToPage(
        @RequestBody request: SiteThemeCreateRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<SiteThemeResponse> {
        val ownerId = httpRequest.getAttribute("userId") as Long
        val response = siteThemeService.applyThemeToPage(request, ownerId)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{siteId}/pages/{pageId}")
    fun updatePageTheme(
        @PathVariable siteId: Long,
        @PathVariable pageId: String,
        @RequestBody request: SiteThemeUpdateRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<SiteThemeResponse> {
        val ownerId = httpRequest.getAttribute("userId") as Long
        val response = siteThemeService.updatePageTheme(siteId, pageId, request, ownerId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{siteId}/pages/{pageId}")
    fun getPageTheme(
        @PathVariable siteId: Long,
        @PathVariable pageId: String
    ): ResponseEntity<SiteThemeResponse> {
        val response = siteThemeService.getPageTheme(siteId, pageId)
        return if (response != null) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{siteId}/pages")
    fun getSitePageThemes(
        @PathVariable siteId: Long,
        httpRequest: HttpServletRequest
    ): ResponseEntity<SitePageThemesResponse> {
        val ownerId = httpRequest.getAttribute("userId") as Long
        val response = siteThemeService.getSitePageThemes(siteId, ownerId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{siteId}/pages/{pageId}/history")
    fun getPageThemeHistory(
        @PathVariable siteId: Long,
        @PathVariable pageId: String,
        httpRequest: HttpServletRequest
    ): ResponseEntity<SiteThemeListResponse> {
        val ownerId = httpRequest.getAttribute("userId") as Long
        val response = siteThemeService.getPageThemeHistory(siteId, pageId, ownerId)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{siteId}/pages/{pageId}")
    fun removePageTheme(
        @PathVariable siteId: Long,
        @PathVariable pageId: String,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Void> {
        val ownerId = httpRequest.getAttribute("userId") as Long
        siteThemeService.removePageTheme(siteId, pageId, ownerId)
        return ResponseEntity.noContent().build()
    }
}
