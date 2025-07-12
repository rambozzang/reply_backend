package com.comdeply.comment.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "테마 정보")
data class ThemeResponse(
    @Schema(description = "테마 ID", example = "1")
    val id: Long,

    @Schema(description = "테마 이름", example = "modern-blue")
    val name: String,

    @Schema(description = "테마 표시 이름", example = "Modern Blue")
    val displayName: String,

    @Schema(description = "테마 설명", example = "깔끔하고 모던한 블루 테마")
    val description: String?,

    @Schema(description = "테마 카테고리", example = "modern")
    val category: String,

    @Schema(description = "시스템 기본 테마 여부", example = "true")
    val isBuiltIn: Boolean,

    @Schema(description = "활성화 상태", example = "true")
    val isActive: Boolean,

    @Schema(description = "프리미엄 테마 여부", example = "false")
    val isPremium: Boolean,

    @Schema(description = "테마 색상 설정")
    val colors: ThemeColors,

    @Schema(description = "타이포그래피 설정")
    val typography: ThemeTypography,

    @Schema(description = "간격 설정")
    val spacing: ThemeSpacing,

    @Schema(description = "테두리 반경 설정")
    val borderRadius: ThemeBorderRadius,

    @Schema(description = "컴포넌트 설정")
    val components: ThemeComponents,

    @Schema(description = "사용자 정의 CSS")
    val customCss: String?,

    @Schema(description = "썸네일 이미지 URL")
    val thumbnailUrl: String?,

    @Schema(description = "미리보기 이미지 URL")
    val previewUrl: String?,

    @Schema(description = "생성자 ID")
    val createdBy: Long?,

    @Schema(description = "생성일시")
    val createdAt: LocalDateTime,

    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime,

    @Schema(description = "사용 횟수")
    val usageCount: Long,

    @Schema(description = "버전")
    val version: String,

    @Schema(description = "태그 목록")
    val tags: List<String>?
)

@Schema(description = "테마 색상 설정")
data class ThemeColors(
    @Schema(description = "기본 색상", example = "#007bff")
    val primary: String,

    @Schema(description = "보조 색상", example = "#6c757d")
    val secondary: String,

    @Schema(description = "성공 색상", example = "#28a745")
    val success: String,

    @Schema(description = "위험 색상", example = "#dc3545")
    val danger: String,

    @Schema(description = "경고 색상", example = "#ffc107")
    val warning: String,

    @Schema(description = "정보 색상", example = "#17a2b8")
    val info: String,

    @Schema(description = "배경 색상", example = "#ffffff")
    val background: String,

    @Schema(description = "표면 색상", example = "#f8f9fa")
    val surface: String,

    @Schema(description = "텍스트 색상", example = "#212529")
    val text: String,

    @Schema(description = "보조 텍스트 색상", example = "#6c757d")
    val textSecondary: String,

    @Schema(description = "테두리 색상", example = "#dee2e6")
    val border: String,

    @Schema(description = "음영 색상", example = "#f8f9fa")
    val muted: String,

    @Schema(description = "어둡게 색상", example = "#343a40")
    val dark: String
)

@Schema(description = "타이포그래피 설정")
data class ThemeTypography(
    @Schema(description = "기본 폰트 패밀리", example = "Inter, -apple-system, sans-serif")
    val fontFamily: String,

    @Schema(description = "폰트 크기")
    val fontSize: FontSizes,

    @Schema(description = "폰트 두께")
    val fontWeight: FontWeights,

    @Schema(description = "줄 간격", example = "1.5")
    val lineHeight: String
)

@Schema(description = "폰트 크기 설정")
data class FontSizes(
    @Schema(description = "작은 크기", example = "12px")
    val sm: String,

    @Schema(description = "기본 크기", example = "14px")
    val md: String,

    @Schema(description = "큰 크기", example = "16px")
    val lg: String,

    @Schema(description = "특대 크기", example = "18px")
    val xl: String
)

@Schema(description = "폰트 두께 설정")
data class FontWeights(
    @Schema(description = "일반 두께", example = "400")
    val normal: String,

    @Schema(description = "중간 두께", example = "500")
    val medium: String,

    @Schema(description = "굵은 두께", example = "600")
    val bold: String
)

@Schema(description = "간격 설정")
data class ThemeSpacing(
    @Schema(description = "작은 간격", example = "8px")
    val sm: String,

    @Schema(description = "기본 간격", example = "16px")
    val md: String,

    @Schema(description = "큰 간격", example = "24px")
    val lg: String,

    @Schema(description = "특대 간격", example = "32px")
    val xl: String
)

@Schema(description = "테두리 반경 설정")
data class ThemeBorderRadius(
    @Schema(description = "작은 반경", example = "4px")
    val sm: String,

    @Schema(description = "기본 반경", example = "8px")
    val md: String,

    @Schema(description = "큰 반경", example = "12px")
    val lg: String,

    @Schema(description = "원형", example = "50%")
    val full: String
)

@Schema(description = "컴포넌트 설정")
data class ThemeComponents(
    @Schema(description = "댓글 위젯 설정")
    val commentWidget: CommentWidgetConfig,

    @Schema(description = "버튼 설정")
    val button: ButtonConfig,

    @Schema(description = "입력 필드 설정")
    val input: InputConfig
)

@Schema(description = "댓글 위젯 설정")
data class CommentWidgetConfig(
    @Schema(description = "최대 너비", example = "800px")
    val maxWidth: String,

    @Schema(description = "패딩", example = "20px")
    val padding: String,

    @Schema(description = "그림자", example = "0 2px 8px rgba(0,0,0,0.1)")
    val boxShadow: String?
)

@Schema(description = "버튼 설정")
data class ButtonConfig(
    @Schema(description = "패딩", example = "8px 16px")
    val padding: String,

    @Schema(description = "높이", example = "40px")
    val height: String,

    @Schema(description = "트랜지션", example = "all 0.2s ease")
    val transition: String?
)

@Schema(description = "입력 필드 설정")
data class InputConfig(
    @Schema(description = "패딩", example = "12px")
    val padding: String,

    @Schema(description = "높이", example = "40px")
    val height: String,

    @Schema(description = "테두리 두께", example = "1px")
    val borderWidth: String
)

// 생성/수정 요청 DTO
@Schema(description = "테마 생성 요청")
data class CreateThemeRequest(
    @Schema(description = "테마 이름", example = "my-custom-theme", required = true)
    val name: String,

    @Schema(description = "테마 표시 이름", example = "My Custom Theme", required = true)
    val displayName: String,

    @Schema(description = "테마 설명", example = "나만의 커스텀 테마")
    val description: String?,

    @Schema(description = "테마 카테고리", example = "custom", required = true)
    val category: String,

    @Schema(description = "프리미엄 테마 여부", example = "false")
    val isPremium: Boolean = false,

    @Schema(description = "테마 색상 설정", required = true)
    val colors: ThemeColors,

    @Schema(description = "타이포그래피 설정", required = true)
    val typography: ThemeTypography,

    @Schema(description = "간격 설정", required = true)
    val spacing: ThemeSpacing,

    @Schema(description = "테두리 반경 설정", required = true)
    val borderRadius: ThemeBorderRadius,

    @Schema(description = "컴포넌트 설정", required = true)
    val components: ThemeComponents,

    @Schema(description = "사용자 정의 CSS")
    val customCss: String?,

    @Schema(description = "태그 목록")
    val tags: List<String>?
)

@Schema(description = "테마 수정 요청")
data class UpdateThemeRequest(
    @Schema(description = "테마 표시 이름", example = "Updated Theme")
    val displayName: String?,

    @Schema(description = "테마 설명", example = "업데이트된 테마 설명")
    val description: String?,

    @Schema(description = "테마 카테고리", example = "modern")
    val category: String?,

    @Schema(description = "활성화 상태", example = "true")
    val isActive: Boolean?,

    @Schema(description = "프리미엄 테마 여부", example = "false")
    val isPremium: Boolean?,

    @Schema(description = "테마 색상 설정")
    val colors: ThemeColors?,

    @Schema(description = "타이포그래피 설정")
    val typography: ThemeTypography?,

    @Schema(description = "간격 설정")
    val spacing: ThemeSpacing?,

    @Schema(description = "테두리 반경 설정")
    val borderRadius: ThemeBorderRadius?,

    @Schema(description = "컴포넌트 설정")
    val components: ThemeComponents?,

    @Schema(description = "사용자 정의 CSS")
    val customCss: String?,

    @Schema(description = "태그 목록")
    val tags: List<String>?
)

@Schema(description = "사이트 테마 적용 요청")
data class ApplySiteThemeRequest(
    @Schema(description = "테마 ID", example = "1", required = true)
    val themeId: Long,

    @Schema(description = "페이지 ID (선택사항, 없으면 사이트 기본 테마로 적용)", example = "/blog/post1")
    val pageId: String?,

    @Schema(description = "커스터마이징 설정")
    val customizations: ThemeCustomizationRequest?
)

@Schema(description = "테마 커스터마이징 설정")
data class ThemeCustomizationRequest(
    @Schema(description = "커스터마이징 이름", example = "My Customization")
    val name: String?,

    @Schema(description = "커스터마이징 설명")
    val description: String?,

    @Schema(description = "커스텀 색상")
    val customColors: ThemeColors?,

    @Schema(description = "커스텀 타이포그래피")
    val customTypography: ThemeTypography?,

    @Schema(description = "커스텀 간격")
    val customSpacing: ThemeSpacing?,

    @Schema(description = "커스텀 테두리 반경")
    val customBorderRadius: ThemeBorderRadius?,

    @Schema(description = "커스텀 컴포넌트")
    val customComponents: ThemeComponents?,

    @Schema(description = "추가 CSS")
    val additionalCss: String?
)
