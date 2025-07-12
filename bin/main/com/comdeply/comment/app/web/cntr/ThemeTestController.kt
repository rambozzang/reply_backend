package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.ThemeService
import com.comdeply.comment.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "테마 테스트", description = "테마 기능 테스트용 API")
@RestController
@RequestMapping("/test/themes")
@CrossOrigin(originPatterns = ["*"])
@Profile("!prod") // 프로덕션 환경에서는 비활성화
class ThemeTestController(
    private val themeService: ThemeService
) {

    @Operation(
        summary = "테스트 테마 생성",
        description = "테스트용 커스텀 테마를 생성합니다."
    )
    @PostMapping("/create-test-theme")
    fun createTestTheme(): ResponseEntity<ThemeResponse> {
        val colors = ThemeColors(
            primary = "#ff6b35",
            secondary = "#f7931e",
            success = "#4caf50",
            danger = "#f44336",
            warning = "#ff9800",
            info = "#2196f3",
            background = "#ffffff",
            surface = "#fff5f5",
            text = "#333333",
            textSecondary = "#666666",
            border = "#e0e0e0",
            muted = "#f5f5f5",
            dark = "#212121"
        )

        val typography = ThemeTypography(
            fontFamily = "Roboto, sans-serif",
            fontSize = FontSizes("12px", "14px", "16px", "20px"),
            fontWeight = FontWeights("300", "400", "700"),
            lineHeight = "1.6"
        )

        val spacing = ThemeSpacing("8px", "16px", "24px", "32px")
        val borderRadius = ThemeBorderRadius("4px", "8px", "16px", "50%")
        val components = ThemeComponents(
            commentWidget = CommentWidgetConfig("800px", "20px", "0 4px 12px rgba(255, 107, 53, 0.15)"),
            button = ButtonConfig("10px 20px", "42px", "all 0.2s ease"),
            input = InputConfig("12px", "42px", "1px")
        )

        val request = CreateThemeRequest(
            name = "test-orange-theme",
            displayName = "테스트 오렌지 테마",
            description = "테스트를 위한 오렌지 컬러 테마입니다.",
            category = "test",
            isPremium = false,
            colors = colors,
            typography = typography,
            spacing = spacing,
            borderRadius = borderRadius,
            components = components,
            customCss = ".comment-widget { border-left: 4px solid #ff6b35; }",
            tags = listOf("테스트", "오렌지", "따뜻한")
        )

        val theme = themeService.createTheme(request, null)
        return ResponseEntity.ok(theme)
    }

    @Operation(
        summary = "모든 테마 카테고리 조회",
        description = "시스템에 있는 모든 테마 카테고리를 조회합니다."
    )
    @GetMapping("/categories")
    fun getAllCategories(): ResponseEntity<Map<String, List<ThemeResponse>>> {
        val allThemes = themeService.getAllActiveThemes()
        val categorized = allThemes.groupBy { it.category }
        return ResponseEntity.ok(categorized)
    }

    @Operation(
        summary = "테마 통계 조회",
        description = "테마 사용 통계를 조회합니다."
    )
    @GetMapping("/stats")
    fun getThemeStats(): ResponseEntity<Map<String, Any>> {
        val allThemes = themeService.getAllActiveThemes()
        val stats = mapOf(
            "totalThemes" to allThemes.size,
            "builtInThemes" to allThemes.count { it.isBuiltIn },
            "customThemes" to allThemes.count { !it.isBuiltIn },
            "premiumThemes" to allThemes.count { it.isPremium },
            "categoryCounts" to allThemes.groupBy { it.category }.mapValues { it.value.size },
            "totalUsage" to allThemes.sumOf { it.usageCount },
            "avgUsagePerTheme" to if (allThemes.isNotEmpty()) {
                allThemes.sumOf { it.usageCount }.toDouble() / allThemes.size
            } else {
                0.0
            }
        )
        return ResponseEntity.ok(stats)
    }

    @Operation(
        summary = "사이트에 테스트 테마 적용",
        description = "특정 사이트에 테스트 테마를 적용합니다."
    )
    @PostMapping("/apply-test-theme/{siteId}")
    fun applyTestTheme(
        @PathVariable siteId: Long,
        @RequestParam(required = false) themeId: Long?
    ): ResponseEntity<Map<String, Any>> {
        val targetThemeId = themeId ?: run {
            // themeId가 없으면 첫 번째 테마 사용
            val themes = themeService.getAllActiveThemes()
            if (themes.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    mapOf("error" to "사용 가능한 테마가 없습니다.")
                )
            }
            themes.first().id
        }

        val request = ApplySiteThemeRequest(
            themeId = targetThemeId,
            pageId = null,
            customizations = null
        )

        return try {
            val siteTheme = themeService.applySiteTheme(siteId, request.pageId, request, 1L)
            val theme = themeService.getThemeById(targetThemeId)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "테마가 성공적으로 적용되었습니다.",
                    "siteId" to siteId,
                    "appliedTheme" to theme,
                    "appliedAt" to siteTheme.appliedAt
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "요청 처리 중 오류가 발생했습니다.")
                )
            )
        }
    }

    @Operation(
        summary = "테마 미리보기 데이터 생성",
        description = "프론트엔드에서 사용할 테마 미리보기 데이터를 생성합니다."
    )
    @GetMapping("/{themeId}/preview-data")
    fun getThemePreviewData(
        @PathVariable themeId: Long
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val theme = themeService.getThemeById(themeId)

            val previewData = mapOf(
                "theme" to theme,
                "sampleComments" to listOf(
                    mapOf(
                        "id" to 1,
                        "author" to "김개발자",
                        "content" to "이 테마 정말 좋네요! 색상이 마음에 듭니다.",
                        "time" to "5분 전",
                        "likes" to 8,
                        "isLiked" to false
                    ),
                    mapOf(
                        "id" to 2,
                        "author" to "박디자이너",
                        "content" to "폰트도 깔끔하고 전체적인 느낌이 세련되어 보이네요.",
                        "time" to "10분 전",
                        "likes" to 12,
                        "isLiked" to true
                    )
                ),
                "cssVariables" to mapOf(
                    "--primary-color" to theme.colors.primary,
                    "--secondary-color" to theme.colors.secondary,
                    "--background-color" to theme.colors.background,
                    "--surface-color" to theme.colors.surface,
                    "--text-color" to theme.colors.text,
                    "--text-secondary-color" to theme.colors.textSecondary,
                    "--border-color" to theme.colors.border,
                    "--font-family" to theme.typography.fontFamily,
                    "--font-size-md" to theme.typography.fontSize.md,
                    "--border-radius-md" to theme.borderRadius.md,
                    "--spacing-md" to theme.spacing.md
                )
            )

            ResponseEntity.ok(previewData)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "테마 검증",
        description = "테마 데이터의 유효성을 검증합니다."
    )
    @PostMapping("/validate")
    fun validateTheme(
        @RequestBody request: CreateThemeRequest
    ): ResponseEntity<Map<String, Any>> {
        val validationErrors = mutableListOf<String>()

        // 기본 유효성 검사
        if (request.name.isBlank()) {
            validationErrors.add("테마 이름은 필수입니다.")
        }
        if (request.displayName.isBlank()) {
            validationErrors.add("테마 표시 이름은 필수입니다.")
        }

        // 색상 유효성 검사
        val colorFields = listOf(
            request.colors.primary,
            request.colors.secondary,
            request.colors.background,
            request.colors.text
        )
        colorFields.forEach { color ->
            if (!color.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                validationErrors.add("잘못된 색상 형식: $color")
            }
        }

        // 이름 중복 검사
        if (themeService.getThemeByName(request.name) != null) {
            validationErrors.add("이미 존재하는 테마 이름입니다: ${request.name}")
        }

        val result = mapOf(
            "isValid" to validationErrors.isEmpty(),
            "errors" to validationErrors,
            "message" to if (validationErrors.isEmpty()) "테마 데이터가 유효합니다." else "유효성 검사 실패"
        )

        return ResponseEntity.ok(result)
    }
}
