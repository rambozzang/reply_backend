package com.comdeply.comment.dto

import java.time.LocalDateTime

data class SiteCreateRequest(
    val siteName: String,
    val domain: String,
    val themeColor: String? = "#007bff",
    val customCss: String? = null,
    val requireAuth: Boolean = false,
    val enableModeration: Boolean = true,
    val theme: String = "light",
    val language: String = "ko",
    val allowedDomains: List<String>? = null // 허용 도메인 목록
)

data class SiteUpdateRequest(
    val siteName: String?,
    val domain: String?,
    val themeColor: String?,
    val customCss: String?,
    val requireAuth: Boolean?,
    val enableModeration: Boolean?,
    val theme: String?,
    val language: String?,
    val isActive: Boolean?,
    val allowedDomains: List<String>? = null // 허용 도메인 목록
)

data class SiteResponse(
    val id: Long,
    val siteName: String,
    val domain: String,
    val ownerId: Long,
    val siteKey: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
    val themeColor: String?,
    val customCss: String?,
    val requireAuth: Boolean,
    val enableModeration: Boolean,
    val theme: String,
    val language: String,
    val embedCode: String,
    val allowedDomains: List<String>? = null // 허용 도메인 목록
)

data class SiteListResponse(
    val sites: List<SiteResponse>,
    val totalCount: Long
)

data class SiteStatsResponseDto(
    val siteId: Long,
    val totalComments: Long,
    val totalUsers: Long,
    val todayComments: Long,
    val popularPages: List<PageStatsResponse>
)

data class PageStatsResponse(
    val pageId: String,
    val commentCount: Long,
    val lastCommentAt: LocalDateTime?
)

// 페이지 관리용 DTO들
data class PageCreateRequest(
    val pageId: String,
    val pageName: String?,
    val pageDescription: String?,
    val pageType: String = "GENERAL", // GENERAL, BOARD, PRODUCT, ARTICLE, OTHER
    val pageUrl: String?
)

data class PageUpdateRequest(
    val pageName: String?,
    val pageDescription: String?,
    val pageType: String?,
    val pageUrl: String?,
    val isActive: Boolean?
)

data class SitePageResponse(
    val id: Long,
    val siteId: String,
    val pageId: String,
    val pageName: String?,
    val pageDescription: String?,
    val pageType: String,
    val pageUrl: String?,
    val commentCount: Long,
    val lastActivityAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isActive: Boolean
)
