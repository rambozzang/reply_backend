package com.comdeply.comment.app.web.svc

import com.comdeply.comment.config.PlanLimits
import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.SiteTheme
import com.comdeply.comment.entity.Theme
import com.comdeply.comment.entity.ThemeCustomization
import com.comdeply.comment.repository.SiteRepository
import com.comdeply.comment.repository.SiteThemeRepository
import com.comdeply.comment.repository.ThemeCustomizationRepository
import com.comdeply.comment.repository.ThemeRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class ThemeService(
    private val themeRepository: ThemeRepository,
    private val siteThemeRepository: SiteThemeRepository,
    private val themeCustomizationRepository: ThemeCustomizationRepository,
    private val siteRepository: SiteRepository,
    private val planValidationService: PlanValidationService,
    private val objectMapper: ObjectMapper
) {

    // 모든 활성 테마 조회
    @Transactional(readOnly = true)
    fun getAllActiveThemes(): List<ThemeResponse> {
        return themeRepository.findByIsActiveTrue().map { convertToDto(it) }
    }

    // 테마 페이징 조회
    @Transactional(readOnly = true)
    fun getThemes(pageable: Pageable): Page<ThemeResponse> {
        return themeRepository.findAll(pageable).map { convertToDto(it) }
    }

    // 테마 ID로 조회
    @Transactional(readOnly = true)
    fun getThemeById(themeId: Long): ThemeResponse {
        val theme = themeRepository.findById(themeId)
            .orElseThrow { IllegalArgumentException("테마를 찾을 수 없습니다: $themeId") }
        return convertToDto(theme)
    }

    // 테마 이름으로 조회
    @Transactional(readOnly = true)
    fun getThemeByName(name: String): ThemeResponse? {
        return themeRepository.findByName(name)?.let { convertToDto(it) }
    }

    // 카테고리별 테마 조회
    @Transactional(readOnly = true)
    fun getThemesByCategory(category: String, pageable: Pageable): Page<ThemeResponse> {
        return themeRepository.findByCategoryAndIsActiveTrueOrderByUsageCountDesc(category, pageable)
            .map { convertToDto(it) }
    }

    // 인기 테마 조회
    @Transactional(readOnly = true)
    fun getPopularThemes(pageable: Pageable): Page<ThemeResponse> {
        return themeRepository.findPopularThemes(pageable).map { convertToDto(it) }
    }

    // 최신 테마 조회
    @Transactional(readOnly = true)
    fun getLatestThemes(pageable: Pageable): Page<ThemeResponse> {
        return themeRepository.findLatestThemes(pageable).map { convertToDto(it) }
    }

    // 기본 테마 조회
    @Transactional(readOnly = true)
    fun getBuiltInThemes(): List<ThemeResponse> {
        return themeRepository.findByIsBuiltInTrueAndIsActiveTrue().map { convertToDto(it) }
    }

    // 프리미엄 테마 조회
    @Transactional(readOnly = true)
    fun getPremiumThemes(): List<ThemeResponse> {
        return themeRepository.findByIsPremiumTrueAndIsActiveTrue().map { convertToDto(it) }
    }

    // 사용자가 생성한 테마 조회
    @Transactional(readOnly = true)
    fun getUserThemes(userId: Long): List<ThemeResponse> {
        return themeRepository.findByCreatedByAndIsActiveTrue(userId).map { convertToDto(it) }
    }

    // 테마 검색
    @Transactional(readOnly = true)
    fun searchThemes(keyword: String, pageable: Pageable): Page<ThemeResponse> {
        return themeRepository.searchThemes(keyword, pageable).map { convertToDto(it) }
    }

    // 테마 생성
    fun createTheme(request: CreateThemeRequest, userId: Long?): ThemeResponse {
        // 사용자별 테마 생성 제한 확인 (사용자가 생성하는 커스텀 테마인 경우)
        if (userId != null) {
            val currentThemeCount = themeRepository.countByCreatedByAndIsActiveTrue(userId)
            val planInfo = planValidationService.getUserPlan(userId)
            val themeLimit = when (planInfo) {
                PlanLimits.Plan.STARTER -> 3
                PlanLimits.Plan.PRO -> 20
                PlanLimits.Plan.ENTERPRISE -> 100
            }

            if (currentThemeCount >= themeLimit) {
                throw IllegalArgumentException("현재 플랜에서는 최대 ${themeLimit}개의 테마만 생성할 수 있습니다. (현재: ${currentThemeCount}개)")
            }
        }

        // 이름 중복 검사
        if (themeRepository.findByName(request.name) != null) {
            throw IllegalArgumentException("이미 존재하는 테마 이름입니다: ${request.name}")
        }

        val theme = Theme(
            name = request.name,
            displayName = request.displayName,
            description = request.description,
            category = request.category,
            isBuiltIn = false,
            isActive = true,
            isPremium = request.isPremium,
            colors = objectMapper.writeValueAsString(request.colors),
            typography = objectMapper.writeValueAsString(request.typography),
            spacing = objectMapper.writeValueAsString(request.spacing),
            borderRadius = objectMapper.writeValueAsString(request.borderRadius),
            components = objectMapper.writeValueAsString(request.components),
            customCss = request.customCss,
            createdBy = userId,
            tags = request.tags?.joinToString(","),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedTheme = themeRepository.save(theme)
        return convertToDto(savedTheme)
    }

    // 테마 수정
    fun updateTheme(themeId: Long, request: UpdateThemeRequest, userId: Long?): ThemeResponse {
        val theme = themeRepository.findById(themeId)
            .orElseThrow { IllegalArgumentException("테마를 찾을 수 없습니다: $themeId") }

        // 권한 검사 (기본 테마가 아니고, 생성자가 다른 경우)
        if (!theme.isBuiltIn && theme.createdBy != userId) {
            throw IllegalAccessException("테마를 수정할 권한이 없습니다.")
        }

        val updatedTheme = theme.copy(
            displayName = request.displayName ?: theme.displayName,
            description = request.description ?: theme.description,
            category = request.category ?: theme.category,
            isActive = request.isActive ?: theme.isActive,
            isPremium = request.isPremium ?: theme.isPremium,
            colors = request.colors?.let { objectMapper.writeValueAsString(it) } ?: theme.colors,
            typography = request.typography?.let { objectMapper.writeValueAsString(it) } ?: theme.typography,
            spacing = request.spacing?.let { objectMapper.writeValueAsString(it) } ?: theme.spacing,
            borderRadius = request.borderRadius?.let { objectMapper.writeValueAsString(it) } ?: theme.borderRadius,
            components = request.components?.let { objectMapper.writeValueAsString(it) } ?: theme.components,
            customCss = request.customCss ?: theme.customCss,
            tags = request.tags?.joinToString(",") ?: theme.tags,
            updatedAt = LocalDateTime.now()
        )

        val savedTheme = themeRepository.save(updatedTheme)
        return convertToDto(savedTheme)
    }

    // 테마 삭제 (비활성화)
    fun deleteTheme(themeId: Long, userId: Long?) {
        val theme = themeRepository.findById(themeId)
            .orElseThrow { IllegalArgumentException("테마를 찾을 수 없습니다: $themeId") }

        // 기본 테마는 삭제할 수 없음
        if (theme.isBuiltIn) {
            throw IllegalArgumentException("기본 테마는 삭제할 수 없습니다.")
        }

        // 권한 검사
        if (theme.createdBy != userId) {
            throw IllegalAccessException("테마를 삭제할 권한이 없습니다.")
        }

        // 사용 중인 사이트가 있는지 확인
        val usingSitesCount = siteThemeRepository.countByThemeIdAndIsActiveTrue(themeId)
        if (usingSitesCount > 0) {
            throw IllegalArgumentException("현재 사용 중인 사이트가 있어 삭제할 수 없습니다. (사용 사이트: $usingSitesCount 개)")
        }

        // 비활성화
        val deactivatedTheme = theme.copy(isActive = false, updatedAt = LocalDateTime.now())
        themeRepository.save(deactivatedTheme)
    }

    // 사이트에 페이지별 테마 적용
    fun applySiteTheme(siteId: Long, pageId: String?, request: ApplySiteThemeRequest, ownerId: Long): SiteTheme {
        // 사이트 소유자 확인
        val site = siteRepository.findById(siteId)
            .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다: $siteId") }

        if (site.ownerId != ownerId) {
            throw IllegalArgumentException("자신의 사이트에만 테마를 적용할 수 있습니다.")
        }

        // 플랜별 테마 적용 제한 확인
        val themeValidation = planValidationService.canApplyTheme(ownerId, siteId)
        if (!themeValidation.isValid) {
            throw IllegalArgumentException(themeValidation.message)
        }

        // 테마 존재 확인
        val theme = themeRepository.findById(request.themeId)
            .orElseThrow { IllegalArgumentException("테마를 찾을 수 없습니다: ${request.themeId}") }

        if (!theme.isActive) {
            throw IllegalArgumentException("비활성화된 테마는 적용할 수 없습니다.")
        }

        val targetPageId = pageId ?: "default" // 페이지 ID가 없으면 기본값 사용

        // 기존 페이지 테마가 있으면 비활성화
        val existingTheme = siteThemeRepository.findBySiteIdAndPageIdAndIsActiveTrue(siteId, targetPageId)
        existingTheme?.let {
            val deactivatedTheme = it.copy(
                isActive = false,
                updatedAt = LocalDateTime.now()
            )
            siteThemeRepository.save(deactivatedTheme)
        }

        // 새 테마 적용
        val siteTheme = SiteTheme(
            siteId = siteId,
            pageId = targetPageId,
            themeId = request.themeId,
            customizations = request.customizations?.let { objectMapper.writeValueAsString(it) },
            isActive = true,
            appliedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedSiteTheme = siteThemeRepository.save(siteTheme)

        // 테마 사용량 증가
        themeRepository.incrementUsageCount(request.themeId)

        return savedSiteTheme
    }

    // 사이트의 기본 테마 적용 (전체 사이트)
    fun applySiteDefaultTheme(siteId: Long, request: ApplySiteThemeRequest, ownerId: Long): SiteTheme {
        return applySiteTheme(siteId, null, request, ownerId)
    }

    // 사이트의 페이지별 현재 테마 조회
    @Transactional(readOnly = true)
    fun getSitePageTheme(siteId: Long, pageId: String): SiteTheme? {
        return siteThemeRepository.findBySiteIdAndPageIdAndIsActiveTrue(siteId, pageId)
    }

    // 사이트의 기본 테마 조회
    @Transactional(readOnly = true)
    fun getSiteDefaultTheme(siteId: Long): SiteTheme? {
        return siteThemeRepository.findBySiteIdAndPageIdAndIsActiveTrue(siteId, "default")
    }

    // 사이트의 모든 활성 테마 조회
    @Transactional(readOnly = true)
    fun getSiteActiveThemes(siteId: Long): List<SiteTheme> {
        return siteThemeRepository.findBySiteIdAndIsActiveTrue(siteId)
    }

    // 사이트의 테마 변경 이력 조회
    @Transactional(readOnly = true)
    fun getSiteThemeHistory(siteId: Long): List<SiteTheme> {
        return siteThemeRepository.findSiteThemeHistory(siteId)
    }

    // 특정 페이지의 테마 변경 이력 조회
    @Transactional(readOnly = true)
    fun getPageThemeHistory(siteId: Long, pageId: String): List<SiteTheme> {
        return siteThemeRepository.findPageThemeHistory(siteId, pageId)
    }

    // 테마 커스터마이징 생성
    fun createThemeCustomization(
        siteId: Long,
        themeId: Long,
        ownerId: Long,
        request: ThemeCustomizationRequest
    ): ThemeCustomization {
        // 사이트 소유자 확인
        val site = siteRepository.findById(siteId)
            .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다: $siteId") }

        if (site.ownerId != ownerId) {
            throw IllegalArgumentException("자신의 사이트에만 테마 커스터마이징을 생성할 수 있습니다.")
        }

        // 이름 중복 검사
        if (request.name != null && themeCustomizationRepository.existsBySiteIdAndNameAndIdNot(siteId, request.name, 0)) {
            throw IllegalArgumentException("이미 존재하는 커스터마이징 이름입니다: ${request.name}")
        }

        val customization = ThemeCustomization(
            siteId = siteId,
            themeId = themeId,
            userId = ownerId,
            name = request.name ?: "커스터마이징 ${LocalDateTime.now()}",
            description = request.description,
            customColors = request.customColors?.let { objectMapper.writeValueAsString(it) },
            customTypography = request.customTypography?.let { objectMapper.writeValueAsString(it) },
            customSpacing = request.customSpacing?.let { objectMapper.writeValueAsString(it) },
            customBorderRadius = request.customBorderRadius?.let { objectMapper.writeValueAsString(it) },
            customComponents = request.customComponents?.let { objectMapper.writeValueAsString(it) },
            additionalCss = request.additionalCss,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        return themeCustomizationRepository.save(customization)
    }

    // 사이트의 커스터마이징 목록 조회
    @Transactional(readOnly = true)
    fun getSiteCustomizations(siteId: Long, ownerId: Long): List<ThemeCustomization> {
        // 사이트 소유자 확인
        val site = siteRepository.findById(siteId)
            .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다: $siteId") }

        if (site.ownerId != ownerId) {
            throw IllegalArgumentException("자신의 사이트의 커스터마이징만 조회할 수 있습니다.")
        }

        return themeCustomizationRepository.findBySiteIdAndIsActiveTrueOrderByCreatedAtDesc(siteId)
    }

    // 엔티티를 DTO로 변환
    private fun convertToDto(theme: Theme): ThemeResponse {
        return ThemeResponse(
            id = theme.id,
            name = theme.name,
            displayName = theme.displayName,
            description = theme.description,
            category = theme.category,
            isBuiltIn = theme.isBuiltIn,
            isActive = theme.isActive,
            isPremium = theme.isPremium,
            colors = objectMapper.readValue(theme.colors.ifEmpty { "{}" }, ThemeColors::class.java),
            typography = objectMapper.readValue(theme.typography.ifEmpty { "{}" }, ThemeTypography::class.java),
            spacing = objectMapper.readValue(theme.spacing.ifEmpty { "{}" }, ThemeSpacing::class.java),
            borderRadius = objectMapper.readValue(theme.borderRadius.ifEmpty { "{}" }, ThemeBorderRadius::class.java),
            components = objectMapper.readValue(theme.components.ifEmpty { "{}" }, ThemeComponents::class.java),
            customCss = theme.customCss,
            thumbnailUrl = theme.thumbnailUrl,
            previewUrl = theme.previewUrl,
            createdBy = theme.createdBy,
            createdAt = theme.createdAt,
            updatedAt = theme.updatedAt,
            usageCount = theme.usageCount,
            version = theme.version,
            tags = theme.tags?.split(",")?.map { tag -> tag.trim() }?.filter { tag -> tag.isNotEmpty() }
        )
    }
}
