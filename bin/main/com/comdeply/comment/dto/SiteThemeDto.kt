package com.comdeply.comment.dto

import java.time.LocalDateTime

data class SiteThemeCreateRequest(
    val siteId: Long,
    val themeId: Long,
    val pageId: String,
    val customizations: String? = null
)

data class SiteThemeUpdateRequest(
    val themeId: Long?,
    val customizations: String?,
    val isActive: Boolean?
)

data class SiteThemeResponse(
    val id: Long,
    val siteId: Long,
    val themeId: Long,
    val pageId: String,
    val customizations: String?,
    val isActive: Boolean,
    val appliedAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val themeName: String? = null,
    val themeDisplayName: String? = null
)

data class SiteThemeListResponse(
    val themes: List<SiteThemeResponse>,
    val totalCount: Long
)

data class PageThemeResponse(
    val pageId: String,
    val theme: SiteThemeResponse?
)

data class SitePageThemesResponse(
    val siteId: Long,
    val pageThemes: List<PageThemeResponse>
)
