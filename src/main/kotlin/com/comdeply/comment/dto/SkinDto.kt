package com.comdeply.comment.dto

import com.comdeply.comment.entity.ApplicationScope
import com.comdeply.comment.entity.SkinType
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

object SkinDto {
    
    data class SkinResponse(
        val id: Long,
        val name: String,
        val description: String,
        val type: String,
        val adminId: Long?,
        val isShared: Boolean,
        val isOwner: Boolean = false,
        val theme: SkinTheme,
        val styles: String,
        val createdAt: String,
        val updatedAt: String
    )

    data class CreateSkinRequest(
        val name: String,
        val description: String,
        val theme: SkinTheme,
        val styles: String
    )

    data class UpdateSkinRequest(
        val name: String? = null,
        val description: String? = null,
        val theme: SkinTheme? = null,
        val styles: String? = null
    )

    data class SkinApplyRequest(
        val siteKey: String,
        val pageId: String?,
        val skinName: String,
        val scope: ApplicationScope
    )

    data class SkinApplyResponse(
        val id: Long,
        val siteKey: String,
        val pageId: String?,
        val skinName: String,
        val scope: ApplicationScope,
        @JsonProperty("createdAt")
        val createdAt: LocalDateTime,
        @JsonProperty("updatedAt")
        val updatedAt: LocalDateTime
    )

    data class SkinBulkApplyRequest(
        val siteKey: String,
        val skinName: String,
        val pageIds: List<String>?, // null이면 전체 사이트에 적용
        val overwriteExisting: Boolean = false // 기존 설정 덮어쓰기 여부
    )

    data class AdminSiteInfo(
        val siteId: String,
        val siteName: String?,
        val siteDescription: String?,
        val permission: String,
        val pageCount: Long,
        val lastActivityAt: LocalDateTime?,
        val pages: List<SitePageInfo> = emptyList()
    )

    data class SitePageInfo(
        val pageId: String,
        val pageName: String?,
        val pageDescription: String?,
        val pageType: String,
        val commentCount: Long,
        val appliedSkin: String?,
        val skinScope: String?,
        val lastActivityAt: LocalDateTime?,
        val isActive: Boolean
    )

    data class SiteManagementResponse(
        val sites: List<AdminSiteInfo>,
        val totalSites: Int,
        val managedSites: Int
    )

    data class PageDiscoveryRequest(
        val siteKey: String,
        val pageId: String,
        val pageUrl: String? = null,
        val pageTitle: String? = null,
        val userAgent: String? = null,
        val referrer: String? = null
    )

    data class SkinTheme(
        val colors: SkinColors,
        val fonts: SkinFonts,
        val spacing: SkinSpacing,
        val borderRadius: SkinBorderRadius
    )

    data class SkinColors(
        val primary: String,
        val primaryHover: String,
        val secondary: String,
        val background: String,
        val surface: String,
        val textPrimary: String,
        val textSecondary: String,
        val border: String
    )

    data class SkinFonts(
        val family: String,
        val sizeBase: String,
        val sizeLg: String
    )

    data class SkinSpacing(
        val sm: String,
        val md: String,
        val lg: String
    )

    data class SkinBorderRadius(
        val sm: String,
        val md: String
    )
}
