package com.comdeply.comment.app.web.svc

import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.SiteTheme
import com.comdeply.comment.repository.SiteRepository
import com.comdeply.comment.repository.SiteThemeRepository
import com.comdeply.comment.repository.ThemeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class SiteThemeService(
    private val siteThemeRepository: SiteThemeRepository,
    private val siteRepository: SiteRepository,
    private val themeRepository: ThemeRepository,
    private val planValidationService: PlanValidationService
) {

    fun applyThemeToPage(request: SiteThemeCreateRequest, ownerId: Long): SiteThemeResponse {
        // 사이트 소유자 확인
        val site = siteRepository.findById(request.siteId)
            .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다.") }

        if (site.ownerId != ownerId) {
            throw IllegalArgumentException("자신의 사이트에만 테마를 적용할 수 있습니다.")
        }

        // 플랜별 테마 적용 제한 확인
        val themeValidation = planValidationService.canApplyTheme(ownerId, request.siteId)
        if (!themeValidation.isValid) {
            throw IllegalArgumentException(themeValidation.message)
        }

        // 테마 존재 확인
        val theme = themeRepository.findById(request.themeId)
            .orElseThrow { IllegalArgumentException("테마를 찾을 수 없습니다.") }

        // 기존 페이지 테마가 있으면 비활성화
        val existingTheme = siteThemeRepository.findBySiteIdAndPageIdAndIsActiveTrue(request.siteId, request.pageId)
        existingTheme?.let {
            val deactivatedTheme = it.copy(
                isActive = false,
                updatedAt = LocalDateTime.now()
            )
            siteThemeRepository.save(deactivatedTheme)
        }

        // 새 테마 적용
        val siteTheme = SiteTheme(
            siteId = request.siteId,
            themeId = request.themeId,
            pageId = request.pageId,
            customizations = request.customizations,
            isActive = true,
            appliedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedSiteTheme = siteThemeRepository.save(siteTheme)
        return toSiteThemeResponse(savedSiteTheme, theme.name, theme.displayName)
    }

    fun updatePageTheme(siteId: Long, pageId: String, request: SiteThemeUpdateRequest, ownerId: Long): SiteThemeResponse {
        // 사이트 소유자 확인
        val site = siteRepository.findById(siteId)
            .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다.") }

        if (site.ownerId != ownerId) {
            throw IllegalArgumentException("자신의 사이트의 테마만 수정할 수 있습니다.")
        }

        // 현재 활성 테마 조회
        val currentTheme = siteThemeRepository.findBySiteIdAndPageIdAndIsActiveTrue(siteId, pageId)
            ?: throw IllegalArgumentException("해당 페이지에 적용된 테마를 찾을 수 없습니다.")

        // 테마 변경이 있는 경우 새 테마 존재 확인
        val theme = if (request.themeId != null) {
            themeRepository.findById(request.themeId)
                .orElseThrow { IllegalArgumentException("테마를 찾을 수 없습니다.") }
        } else {
            themeRepository.findById(currentTheme.themeId)
                .orElseThrow { IllegalArgumentException("현재 테마를 찾을 수 없습니다.") }
        }

        // 테마 업데이트
        val updatedTheme = currentTheme.copy(
            themeId = request.themeId ?: currentTheme.themeId,
            customizations = request.customizations ?: currentTheme.customizations,
            isActive = request.isActive ?: currentTheme.isActive,
            updatedAt = LocalDateTime.now()
        )

        val savedSiteTheme = siteThemeRepository.save(updatedTheme)
        return toSiteThemeResponse(savedSiteTheme, theme.name, theme.displayName)
    }

    fun getPageTheme(siteId: Long, pageId: String): SiteThemeResponse? {
        val siteTheme = siteThemeRepository.findBySiteIdAndPageIdAndIsActiveTrue(siteId, pageId)
            ?: return null

        val theme = themeRepository.findById(siteTheme.themeId)
            .orElseThrow { IllegalArgumentException("테마를 찾을 수 없습니다.") }

        return toSiteThemeResponse(siteTheme, theme.name, theme.displayName)
    }

    fun getSitePageThemes(siteId: Long, ownerId: Long): SitePageThemesResponse {
        // 사이트 소유자 확인
        val site = siteRepository.findById(siteId)
            .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다.") }

        if (site.ownerId != ownerId) {
            throw IllegalArgumentException("자신의 사이트의 테마만 조회할 수 있습니다.")
        }

        val siteThemes = siteThemeRepository.findBySiteIdAndIsActiveTrue(siteId)
        val pageThemes = siteThemes.map { siteTheme ->
            val theme = themeRepository.findById(siteTheme.themeId)
                .orElse(null)

            PageThemeResponse(
                pageId = siteTheme.pageId,
                theme = theme?.let { toSiteThemeResponse(siteTheme, it.name, it.displayName) }
            )
        }

        return SitePageThemesResponse(
            siteId = siteId,
            pageThemes = pageThemes
        )
    }

    fun getPageThemeHistory(siteId: Long, pageId: String, ownerId: Long): SiteThemeListResponse {
        // 사이트 소유자 확인
        val site = siteRepository.findById(siteId)
            .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다.") }

        if (site.ownerId != ownerId) {
            throw IllegalArgumentException("자신의 사이트의 테마 히스토리만 조회할 수 있습니다.")
        }

        val themeHistory = siteThemeRepository.findPageThemeHistory(siteId, pageId)
        val themeResponses = themeHistory.map { siteTheme ->
            val theme = themeRepository.findById(siteTheme.themeId)
                .orElse(null)

            toSiteThemeResponse(siteTheme, theme?.name, theme?.displayName)
        }

        return SiteThemeListResponse(
            themes = themeResponses,
            totalCount = themeHistory.size.toLong()
        )
    }

    fun removePageTheme(siteId: Long, pageId: String, ownerId: Long) {
        // 사이트 소유자 확인
        val site = siteRepository.findById(siteId)
            .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다.") }

        if (site.ownerId != ownerId) {
            throw IllegalArgumentException("자신의 사이트의 테마만 제거할 수 있습니다.")
        }

        // 현재 활성 테마 비활성화
        val currentTheme = siteThemeRepository.findBySiteIdAndPageIdAndIsActiveTrue(siteId, pageId)
            ?: throw IllegalArgumentException("해당 페이지에 적용된 테마를 찾을 수 없습니다.")

        val deactivatedTheme = currentTheme.copy(
            isActive = false,
            updatedAt = LocalDateTime.now()
        )

        siteThemeRepository.save(deactivatedTheme)
    }

    private fun toSiteThemeResponse(
        siteTheme: SiteTheme,
        themeName: String?,
        themeDisplayName: String?
    ): SiteThemeResponse {
        return SiteThemeResponse(
            id = siteTheme.id,
            siteId = siteTheme.siteId,
            themeId = siteTheme.themeId,
            pageId = siteTheme.pageId,
            customizations = siteTheme.customizations,
            isActive = siteTheme.isActive,
            appliedAt = siteTheme.appliedAt,
            updatedAt = siteTheme.updatedAt,
            themeName = themeName,
            themeDisplayName = themeDisplayName
        )
    }
}
