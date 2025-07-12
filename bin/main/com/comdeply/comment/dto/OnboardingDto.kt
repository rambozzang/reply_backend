package com.comdeply.comment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// =============== 공통 응답 ===============

@Schema(description = "온보딩 공통 응답")
data class OnboardingResponse(
    @Schema(description = "성공 여부")
    val success: Boolean,

    @Schema(description = "응답 메시지")
    val message: String,

    @Schema(description = "응답 데이터")
    val data: Any?
)

// =============== 1단계: 관리자 등록 ===============

@Schema(description = "관리자 등록 요청")
data class OnboardingAdminRegisterRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @Schema(description = "이메일", example = "admin@example.com")
    val email: String,

    @field:NotBlank(message = "사용자명은 필수입니다")
    @field:Size(min = 3, max = 50, message = "사용자명은 3-50자여야 합니다")
    @Schema(description = "사용자명", example = "admin123")
    val username: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, message = "비밀번호는 최소 8자여야 합니다")
    @Schema(description = "비밀번호", example = "password123!")
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(max = 50, message = "이름은 최대 50자입니다")
    @Schema(description = "이름", example = "홍길동")
    val name: String,

    @Schema(description = "조직명", example = "내 회사")
    val organization: String? = null
)

@Schema(description = "관리자 등록 응답")
data class OnboardingAdminResponse(
    @Schema(description = "관리자 ID")
    val adminId: Long,

    @Schema(description = "사용자명")
    val username: String,

    @Schema(description = "이메일")
    val email: String,

    @Schema(description = "이름")
    val name: String,

    @Schema(description = "JWT 토큰")
    val token: String,

    @Schema(description = "토큰 만료 시간 (초)")
    val expiresIn: Long
)

// =============== 1단계: 관리자 로그인 ===============
@Schema(description = "관리자 로그인 요청")
data class OnboardingAdminLoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @Schema(description = "사용자명")
    val username: String,
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @Schema(description = "비밀번호")
    val password: String
)

// =============== 2단계: 사이트 등록 ===============

@Schema(description = "사이트 등록 요청")
data class OnboardingSiteRegisterRequest(
    @field:NotBlank(message = "사이트명은 필수입니다")
    @field:Size(max = 100, message = "사이트명은 최대 100자입니다")
    @Schema(description = "사이트명", example = "내 블로그")
    val siteName: String,

    @field:NotBlank(message = "도메인은 필수입니다")
    @field:Size(max = 255, message = "도메인은 최대 255자입니다")
    @Schema(description = "도메인", example = "myblog.com")
    val domain: String,

    @Schema(description = "테마 색상", example = "#007bff")
    val themeColor: String? = "#007bff",

    @Schema(description = "인증 필요 여부", example = "false")
    val requireAuth: Boolean? = false,

    @Schema(description = "모더레이션 활성화 여부", example = "true")
    val enableModeration: Boolean? = true
)

@Schema(description = "사이트 등록 응답")
data class OnboardingSiteResponse(
    @Schema(description = "사이트 ID")
    val siteId: Long,

    @Schema(description = "사이트 키")
    val siteKey: String,

    @Schema(description = "사이트명")
    val siteName: String,

    @Schema(description = "도메인")
    val domain: String,

    @Schema(description = "테마 색상")
    val themeColor: String,

    @Schema(description = "인증 필요 여부")
    val requireAuth: Boolean,

    @Schema(description = "모더레이션 활성화 여부")
    val enableModeration: Boolean
)

// =============== 2단계: 스킨 적용 ===============

@Schema(description = "스킨 적용 요청")
data class OnboardingSkinApplyRequest(
    @field:NotBlank(message = "사이트 키는 필수입니다")
    @Schema(description = "사이트 키")
    val siteKey: String,

    @field:NotBlank(message = "스킨명은 필수입니다")
    @Schema(description = "스킨명", example = "modern")
    val skinName: String,

    @Schema(description = "페이지 ID", example = "default")
    val pageId: String? = "default",

    @Schema(description = "커스터마이징 설정 (JSON)", example = "{}")
    val customizations: String? = "{}"
)

@Schema(description = "스킨 적용 응답")
data class OnboardingSkinResponse(
    @Schema(description = "사이트 테마 ID")
    val siteThemeId: Long,

    @Schema(description = "사이트 키")
    val siteKey: String,

    @Schema(description = "테마명")
    val themeName: String,

    @Schema(description = "테마 표시명")
    val themeDisplayName: String,

    @Schema(description = "페이지 ID")
    val pageId: String,

    @Schema(description = "적용 시간")
    val appliedAt: LocalDateTime
)

// =============== 3단계: 구독 생성 ===============

@Schema(description = "무료 구독 생성 요청")
data class OnboardingFreeSubscriptionRequest(
    @field:NotBlank(message = "플랜 ID는 필수입니다")
    @Schema(description = "플랜 ID", example = "starter")
    val planId: String = "starter"
)

@Schema(description = "유료 구독 생성 요청")
data class OnboardingPaidSubscriptionRequest(
    @field:NotBlank(message = "플랜 ID는 필수입니다")
    @Schema(description = "플랜 ID", example = "pro")
    val planId: String,

    @field:NotBlank(message = "결제 수단은 필수입니다")
    @Schema(description = "결제 수단", example = "card")
    val paymentMethod: String,

    @field:NotBlank(message = "고객명은 필수입니다")
    @Schema(description = "고객명", example = "홍길동")
    val customerName: String,

    @field:NotBlank(message = "고객 이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @Schema(description = "고객 이메일", example = "customer@example.com")
    val customerEmail: String,

    @Schema(description = "고객 전화번호", example = "010-1234-5678")
    val customerPhone: String? = null,

    // 카드 정보 필드 추가
    @field:NotBlank(message = "카드번호는 필수입니다")
    @Schema(description = "카드번호 (하이픈 제거)", example = "1234567890123456")
    val cardNumber: String,

    @field:NotBlank(message = "유효기간은 필수입니다")
    @Schema(description = "유효기간 (MMYY)", example = "1225")
    val expiry: String,

    @field:NotBlank(message = "생년월일은 필수입니다")
    @Schema(description = "생년월일 (YYMMDD)", example = "900101")
    val birth: String,

    @field:NotBlank(message = "비밀번호 앞 2자리는 필수입니다")
    @Schema(description = "비밀번호 앞 2자리", example = "12")
    val pwd2digit: String
)

@Schema(description = "구독 생성 응답")
data class OnboardingSubscriptionResponse(
    @Schema(description = "구독 ID")
    val subscriptionId: Long,

    @Schema(description = "플랜 ID")
    val planId: String,

    @Schema(description = "플랜명")
    val planName: String,

    @Schema(description = "월 요금")
    val amount: Int,

    @Schema(description = "구독 상태")
    val status: String,

    @Schema(description = "시작 날짜")
    val startDate: LocalDateTime,

    @Schema(description = "종료 날짜")
    val endDate: LocalDateTime?,

    @Schema(description = "월간 댓글 제한")
    val monthlyCommentLimit: Int
)

@Schema(description = "유료 구독 생성 응답")
data class OnboardingPaidSubscriptionResponse(
    @Schema(description = "구독 ID")
    val subscriptionId: Long,

    @Schema(description = "플랜 ID")
    val planId: String,

    @Schema(description = "플랜명")
    val planName: String,

    @Schema(description = "월 요금")
    val amount: Int,

    @Schema(description = "구독 상태")
    val status: String,

    @Schema(description = "시작 날짜")
    val startDate: LocalDateTime,

    @Schema(description = "종료 날짜")
    val endDate: LocalDateTime?,

    @Schema(description = "다음 청구 날짜")
    val nextBillingDate: LocalDateTime?,

    @Schema(description = "월간 댓글 제한")
    val monthlyCommentLimit: Int,

    @Schema(description = "결제 ID")
    val paymentId: Long,

    @Schema(description = "결제 상태")
    val paymentStatus: String
)

// =============== 4단계: 완료 정보 ===============

@Schema(description = "완료 정보 응답")
data class OnboardingCompletionResponse(
    @Schema(description = "사이트 키")
    val siteKey: String,

    @Schema(description = "사이트명")
    val siteName: String,

    @Schema(description = "도메인")
    val domain: String,

    @Schema(description = "적용된 테마명")
    val themeName: String,

    @Schema(description = "선택된 플랜명")
    val planName: String,

    @Schema(description = "임베드 코드")
    val embedCode: String,

    @Schema(description = "대시보드 URL")
    val dashboardUrl: String,

    @Schema(description = "문서 URL")
    val documentationUrl: String
)

// =============== 기타 ===============

@Schema(description = "테마 정보 응답")
data class OnboardingThemeResponse(
    @Schema(description = "테마 ID")
    val id: Long,

    @Schema(description = "테마명")
    val name: String,

    @Schema(description = "테마 표시명")
    val displayName: String,

    @Schema(description = "테마 설명")
    val description: String,

    @Schema(description = "카테고리")
    val category: String,

    @Schema(description = "썸네일 URL")
    val thumbnailUrl: String?,

    @Schema(description = "프리미엄 여부")
    val isPremium: Boolean,

    @Schema(description = "기본 테마 여부")
    val isBuiltIn: Boolean
)

// =============== 2단계: 사이트 및 스킨 통합 등록 ===============

@Schema(description = "사이트 및 스킨 통합 등록 요청")
data class OnboardingSiteWithSkinRequest(
    @field:NotBlank(message = "사이트명은 필수입니다")
    @field:Size(max = 100, message = "사이트명은 최대 100자입니다")
    @Schema(description = "사이트명", example = "내 블로그")
    val siteName: String,

    @field:NotBlank(message = "도메인은 필수입니다")
    @field:Size(max = 255, message = "도메인은 최대 255자입니다")
    @Schema(description = "도메인", example = "myblog.com")
    val domain: String,

    @Schema(description = "테마 색상", example = "#007bff")
    val themeColor: String? = "#007bff",

    @Schema(description = "인증 필요 여부", example = "false")
    val requireAuth: Boolean? = false,

    @Schema(description = "모더레이션 활성화 여부", example = "true")
    val enableModeration: Boolean? = true,

    @field:NotBlank(message = "스킨명은 필수입니다")
    @Schema(description = "스킨명", example = "modern")
    val skinName: String,

    @Schema(description = "페이지 ID", example = "default")
    val pageId: String? = "default",

    @Schema(description = "커스터마이징 설정 (JSON)", example = "{}")
    val customizations: String? = "{}"
)

@Schema(description = "사이트 및 스킨 통합 등록 응답")
data class OnboardingSiteWithSkinResponse(
    @Schema(description = "사이트 등록 정보")
    val site: OnboardingSiteResponse,

    @Schema(description = "스킨 적용 정보")
    val skin: OnboardingSkinResponse
)

@Schema(description = "고객 정보")
data class CustomerInfo(
    @Schema(description = "고객명")
    val name: String,

    @Schema(description = "고객 이메일")
    val email: String,

    @Schema(description = "고객 전화번호")
    val phone: String? = null
)

@Schema(description = "결제 생성 요청")
data class PaymentCreateRequest(
    @Schema(description = "플랜 ID")
    val planId: String,

    @Schema(description = "플랜명")
    val planName: String,

    @Schema(description = "결제 금액")
    val amount: Int,

    @Schema(description = "결제 수단")
    val paymentMethod: String,

    @Schema(description = "고객 정보")
    val customerInfo: CustomerInfo
)
