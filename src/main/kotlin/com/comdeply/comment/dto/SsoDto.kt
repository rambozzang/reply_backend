package com.comdeply.comment.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "SSO 인증 요청")
data class SsoAuthRequest(
    @Schema(description = "사이트 ID", example = "1", required = true)
    val siteId: Long,

    @Schema(description = "암호화된 사용자 정보 토큰", required = true)
    val userToken: String,

    @Schema(description = "타임스탬프 (토큰 재사용 방지)", example = "1641024000", required = true)
    val timestamp: Long,

    @Schema(description = "서명 (HMAC-SHA256)", required = true)
    val signature: String
)

@Schema(description = "고객 사이트 사용자 정보")
data class ExternalUserInfo(
    @Schema(description = "고객 사이트의 사용자 고유 ID", example = "user123", required = true)
    val userId: String,

    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    val name: String,

    @Schema(description = "이메일 주소", example = "user@example.com")
    val email: String? = null,

    @Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String? = null,

    @Schema(description = "추가 사용자 정보")
    val metadata: Map<String, Any>? = null
)

@Schema(description = "SSO 인증 응답")
data class SsoAuthResponse(
    @Schema(description = "인증 성공 여부", example = "true")
    val success: Boolean,

    @Schema(description = "액세스 토큰 (JWT)")
    val accessToken: String? = null,

    @Schema(description = "리프레시 토큰")
    val refreshToken: String? = null,

    @Schema(description = "토큰 만료 시간 (초)")
    val expiresIn: Long? = null,

    @Schema(description = "사용자 정보")
    val user: SsoUserResponse? = null,

    @Schema(description = "오류 메시지")
    val error: String? = null,

    @Schema(description = "오류 코드")
    val errorCode: String? = null
)

@Schema(description = "SSO 사용자 정보")
data class SsoUserResponse(
    @Schema(description = "내부 사용자 ID")
    val id: Long,

    @Schema(description = "사용자 이름")
    val name: String,

    @Schema(description = "이메일 주소")
    val email: String?,

    @Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String?,

    @Schema(description = "외부 사용자 ID")
    val externalUserId: String,

    @Schema(description = "마지막 로그인 시간")
    val lastLoginAt: LocalDateTime
)

@Schema(description = "SSO 토큰 갱신 요청")
data class SsoRefreshRequest(
    @Schema(description = "리프레시 토큰", required = true)
    val refreshToken: String
)

@Schema(description = "SSO 설정 요청")
data class SsoConfigRequest(
    @Schema(description = "SSO 활성화 여부", example = "true")
    val enabled: Boolean = true,

    @Schema(description = "토큰 만료 시간 (분)", example = "60")
    val tokenExpirationMinutes: Int = 60,

    @Schema(description = "허용된 도메인 목록")
    val allowedDomains: List<String>? = null,

    @Schema(description = "HMAC 서명 키 (선택사항, 자동 생성됨)")
    val hmacSecretKey: String? = null
)

@Schema(description = "SSO 설정 응답")
data class SsoConfigResponse(
    @Schema(description = "사이트 ID")
    val siteId: Long,

    @Schema(description = "SSO 활성화 여부")
    val enabled: Boolean,

    @Schema(description = "HMAC 서명 키")
    val hmacSecretKey: String,

    @Schema(description = "토큰 만료 시간 (분)")
    val tokenExpirationMinutes: Int,

    @Schema(description = "허용된 도메인 목록")
    val allowedDomains: List<String>?,

    @Schema(description = "SSO 엔드포인트 URL")
    val ssoEndpoint: String,

    @Schema(description = "설정 업데이트 시간")
    val updatedAt: LocalDateTime
)

@Schema(description = "SSO 사용자 목록 응답 (Deprecated: PageResponse<SsoUserResponse> 사용)")
@Deprecated("Use PageResponse<SsoUserResponse> instead")
data class SsoUserListResponse(
    @Schema(description = "사용자 목록")
    val users: List<SsoUserResponse>,

    @Schema(description = "전체 사용자 수")
    val totalCount: Long,

    @Schema(description = "현재 페이지")
    val currentPage: Int,

    @Schema(description = "페이지 크기")
    val pageSize: Int
)

@Schema(description = "SSO 세션 정보")
data class SsoSessionResponse(
    @Schema(description = "세션 ID")
    val id: Long,

    @Schema(description = "SSO 사용자 ID")
    val ssoUserId: Long,

    @Schema(description = "사용자 이름")
    val userName: String,

    @Schema(description = "세션 토큰")
    val sessionToken: String?,

    @Schema(description = "활성 상태")
    val isActive: Boolean,

    @Schema(description = "만료 시간")
    val expiresAt: LocalDateTime,

    @Schema(description = "생성 시간")
    val createdAt: LocalDateTime,

    @Schema(description = "마지막 활동 시간")
    val lastActivityAt: LocalDateTime?,

    @Schema(description = "IP 주소")
    val ipAddress: String?,

    @Schema(description = "사용자 에이전트")
    val userAgent: String?
)

@Schema(description = "SSO 통계 정보")
data class SsoStatsResponse(
    @Schema(description = "총 SSO 사용자 수")
    val totalSsoUsers: Long,

    @Schema(description = "활성 세션 수")
    val activeSessions: Long,

    @Schema(description = "오늘 로그인 수")
    val todayLogins: Long,

    @Schema(description = "이번 주 로그인 수")
    val weeklyLogins: Long,

    @Schema(description = "이번 달 로그인 수")
    val monthlyLogins: Long,

    @Schema(description = "평균 세션 시간 (분)")
    val avgSessionDuration: Double,

    @Schema(description = "인기 브라우저 통계")
    val browserStats: Map<String, Long>,

    @Schema(description = "일별 로그인 통계 (최근 7일)")
    val dailyLoginStats: Map<String, Long>
)
