package com.comdeply.comment.app.web.svc

import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.Theme
import com.comdeply.comment.repository.ThemeRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ThemeInitializationService(
    private val themeRepository: ThemeRepository,
    private val objectMapper: ObjectMapper
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        initializeDefaultThemes()
    }

    private fun initializeDefaultThemes() {
        if (themeRepository.count() > 0) {
            return // 이미 테마가 있으면 초기화하지 않음
        }

        // 기본 테마들 생성
        createModernTheme()
        createMinimalTheme()
        createDarkTheme()
        createColorfulTheme()
        createCorporateTheme()
        createSimpleTheme()
    }

    private fun createModernTheme() {
        val colors = ThemeColors(
            primary = "#007bff",
            secondary = "#6c757d",
            success = "#28a745",
            danger = "#dc3545",
            warning = "#ffc107",
            info = "#17a2b8",
            background = "#ffffff",
            surface = "#f8f9fa",
            text = "#212529",
            textSecondary = "#6c757d",
            border = "#dee2e6",
            muted = "#f8f9fa",
            dark = "#343a40"
        )

        val typography = ThemeTypography(
            fontFamily = "Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
            fontSize = FontSizes("12px", "14px", "16px", "18px"),
            fontWeight = FontWeights("400", "500", "600"),
            lineHeight = "1.5"
        )

        val spacing = ThemeSpacing("8px", "16px", "24px", "32px")
        val borderRadius = ThemeBorderRadius("4px", "8px", "12px", "50%")
        val components = ThemeComponents(
            commentWidget = CommentWidgetConfig("800px", "20px", "0 2px 8px rgba(0,0,0,0.1)"),
            button = ButtonConfig("8px 16px", "40px", "all 0.2s ease"),
            input = InputConfig("12px", "40px", "1px")
        )

        val theme = Theme(
            name = "modern",
            displayName = "모던",
            description = "현대적이고 세련된 디자인의 테마입니다.",
            category = "general",
            isBuiltIn = true,
            isActive = true,
            isPremium = false,
            colors = objectMapper.writeValueAsString(colors),
            typography = objectMapper.writeValueAsString(typography),
            spacing = objectMapper.writeValueAsString(spacing),
            borderRadius = objectMapper.writeValueAsString(borderRadius),
            components = objectMapper.writeValueAsString(components),
            tags = "모던,현대적,세련된,깔끔"
        )

        themeRepository.save(theme)
    }

    private fun createMinimalTheme() {
        val colors = ThemeColors(
            primary = "#4f46e5",
            secondary = "#6b7280",
            success = "#10b981",
            danger = "#ef4444",
            warning = "#f59e0b",
            info = "#3b82f6",
            background = "#ffffff",
            surface = "#f9fafb",
            text = "#111827",
            textSecondary = "#6b7280",
            border = "#e5e7eb",
            muted = "#f3f4f6",
            dark = "#1f2937"
        )

        val typography = ThemeTypography(
            fontFamily = "Pretendard, -apple-system, BlinkMacSystemFont, system-ui, Roboto, sans-serif",
            fontSize = FontSizes("13px", "15px", "17px", "19px"),
            fontWeight = FontWeights("400", "500", "700"),
            lineHeight = "1.6"
        )

        val spacing = ThemeSpacing("10px", "18px", "28px", "36px")
        val borderRadius = ThemeBorderRadius("6px", "10px", "16px", "50%")
        val components = ThemeComponents(
            commentWidget = CommentWidgetConfig("900px", "24px", "0 4px 16px rgba(79, 70, 229, 0.1)"),
            button = ButtonConfig("10px 20px", "44px", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)"),
            input = InputConfig("14px", "44px", "2px")
        )

        val theme = Theme(
            name = "minimal",
            displayName = "미니멀",
            description = "심플하고 깔끔한 미니멀리즘 테마입니다.",
            category = "general",
            isBuiltIn = true,
            isActive = true,
            isPremium = false,
            colors = objectMapper.writeValueAsString(colors),
            typography = objectMapper.writeValueAsString(typography),
            spacing = objectMapper.writeValueAsString(spacing),
            borderRadius = objectMapper.writeValueAsString(borderRadius),
            components = objectMapper.writeValueAsString(components),
            tags = "미니멀,심플,깔끔,간결"
        )

        themeRepository.save(theme)
    }

    private fun createDarkTheme() {
        val colors = ThemeColors(
            primary = "#6366f1",
            secondary = "#64748b",
            success = "#22c55e",
            danger = "#ef4444",
            warning = "#eab308",
            info = "#06b6d4",
            background = "#0f172a",
            surface = "#1e293b",
            text = "#f1f5f9",
            textSecondary = "#94a3b8",
            border = "#334155",
            muted = "#1e293b",
            dark = "#020617"
        )

        val typography = ThemeTypography(
            fontFamily = "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
            fontSize = FontSizes("12px", "14px", "16px", "18px"),
            fontWeight = FontWeights("300", "400", "600"),
            lineHeight = "1.5"
        )

        val spacing = ThemeSpacing("6px", "12px", "20px", "28px")
        val borderRadius = ThemeBorderRadius("2px", "4px", "8px", "50%")
        val components = ThemeComponents(
            commentWidget = CommentWidgetConfig("750px", "16px", "0 1px 3px rgba(0,0,0,0.05)"),
            button = ButtonConfig("6px 12px", "36px", "all 0.15s ease"),
            input = InputConfig("10px", "36px", "1px")
        )

        val theme = Theme(
            name = "dark",
            displayName = "다크",
            description = "눈이 편안한 다크 모드 테마입니다.",
            category = "general",
            isBuiltIn = true,
            isActive = true,
            isPremium = false,
            colors = objectMapper.writeValueAsString(colors),
            typography = objectMapper.writeValueAsString(typography),
            spacing = objectMapper.writeValueAsString(spacing),
            borderRadius = objectMapper.writeValueAsString(borderRadius),
            components = objectMapper.writeValueAsString(components),
            tags = "다크,어두운,야간,편안한"
        )

        themeRepository.save(theme)
    }

    private fun createColorfulTheme() {
        val colors = ThemeColors(
            primary = "#8b5cf6",
            secondary = "#06b6d4",
            success = "#10b981",
            danger = "#f43f5e",
            warning = "#f59e0b",
            info = "#3b82f6",
            background = "#fefefe",
            surface = "#faf5ff",
            text = "#1e1b4b",
            textSecondary = "#7c3aed",
            border = "#c4b5fd",
            muted = "#f3f4f6",
            dark = "#312e81"
        )

        val typography = ThemeTypography(
            fontFamily = "Nunito, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
            fontSize = FontSizes("13px", "15px", "17px", "19px"),
            fontWeight = FontWeights("400", "600", "700"),
            lineHeight = "1.7"
        )

        val spacing = ThemeSpacing("10px", "20px", "30px", "40px")
        val borderRadius = ThemeBorderRadius("8px", "12px", "20px", "50%")
        val components = ThemeComponents(
            commentWidget = CommentWidgetConfig("850px", "25px", "0 8px 24px rgba(139, 92, 246, 0.15)"),
            button = ButtonConfig("12px 24px", "46px", "all 0.3s ease"),
            input = InputConfig("16px", "46px", "2px")
        )

        val theme = Theme(
            name = "colorful",
            displayName = "컬러풀",
            description = "활기차고 다채로운 컬러의 테마입니다.",
            category = "colorful",
            isBuiltIn = true,
            isActive = true,
            isPremium = false,
            colors = objectMapper.writeValueAsString(colors),
            typography = objectMapper.writeValueAsString(typography),
            spacing = objectMapper.writeValueAsString(spacing),
            borderRadius = objectMapper.writeValueAsString(borderRadius),
            components = objectMapper.writeValueAsString(components),
            tags = "컬러풀,활기찬,다채로운,화려한"
        )

        themeRepository.save(theme)
    }

    private fun createCorporateTheme() {
        val colors = ThemeColors(
            primary = "#1e40af",
            secondary = "#475569",
            success = "#047857",
            danger = "#b91c1c",
            warning = "#92400e",
            info = "#1e3a8a",
            background = "#ffffff",
            surface = "#f8fafc",
            text = "#0f172a",
            textSecondary = "#475569",
            border = "#cbd5e1",
            muted = "#f1f5f9",
            dark = "#0f172a"
        )

        val typography = ThemeTypography(
            fontFamily = "Georgia, 'Times New Roman', Times, serif",
            fontSize = FontSizes("12px", "14px", "16px", "18px"),
            fontWeight = FontWeights("400", "500", "700"),
            lineHeight = "1.5"
        )

        val spacing = ThemeSpacing("8px", "16px", "24px", "32px")
        val borderRadius = ThemeBorderRadius("2px", "4px", "6px", "50%")
        val components = ThemeComponents(
            commentWidget = CommentWidgetConfig("780px", "18px", "0 2px 4px rgba(0,0,0,0.05)"),
            button = ButtonConfig("8px 14px", "38px", "all 0.15s ease"),
            input = InputConfig("10px", "38px", "1px")
        )

        val theme = Theme(
            name = "corporate",
            displayName = "코포레이트",
            description = "기업 환경에 적합한 전문적인 테마입니다.",
            category = "general",
            isBuiltIn = true,
            isActive = true,
            isPremium = false,
            colors = objectMapper.writeValueAsString(colors),
            typography = objectMapper.writeValueAsString(typography),
            spacing = objectMapper.writeValueAsString(spacing),
            borderRadius = objectMapper.writeValueAsString(borderRadius),
            components = objectMapper.writeValueAsString(components),
            tags = "코포레이트,비즈니스,전문적,신뢰감"
        )

        themeRepository.save(theme)
    }

    private fun createSimpleTheme() {
        val colors = ThemeColors(
            primary = "#333333",
            secondary = "#666666",
            success = "#4caf50",
            danger = "#f44336",
            warning = "#ff9800",
            info = "#2196f3",
            background = "#ffffff",
            surface = "#fafafa",
            text = "#212121",
            textSecondary = "#757575",
            border = "#e0e0e0",
            muted = "#f5f5f5",
            dark = "#212121"
        )

        val typography = ThemeTypography(
            fontFamily = "Arial, Helvetica, sans-serif",
            fontSize = FontSizes("12px", "14px", "16px", "18px"),
            fontWeight = FontWeights("400", "500", "600"),
            lineHeight = "1.5"
        )

        val spacing = ThemeSpacing("8px", "16px", "24px", "32px")
        val borderRadius = ThemeBorderRadius("0px", "2px", "4px", "50%")
        val components = ThemeComponents(
            commentWidget = CommentWidgetConfig("760px", "16px", "none"),
            button = ButtonConfig("8px 16px", "38px", "all 0.1s ease"),
            input = InputConfig("10px", "38px", "1px")
        )

        val theme = Theme(
            name = "simple",
            displayName = "심플",
            description = "간단하고 깨끗한 기본적인 테마입니다.",
            category = "general",
            isBuiltIn = true,
            isActive = true,
            isPremium = false,
            colors = objectMapper.writeValueAsString(colors),
            typography = objectMapper.writeValueAsString(typography),
            spacing = objectMapper.writeValueAsString(spacing),
            borderRadius = objectMapper.writeValueAsString(borderRadius),
            components = objectMapper.writeValueAsString(components),
            tags = "심플,간단,기본,깨끗"
        )

        themeRepository.save(theme)
    }
}
