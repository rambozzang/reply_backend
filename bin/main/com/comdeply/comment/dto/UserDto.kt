package com.comdeply.comment.dto

import com.comdeply.comment.entity.UserRole
import com.comdeply.comment.entity.UserType
import java.time.LocalDateTime

// 사용자 응답 (사이트 정보 포함)
data class UserWithSiteResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
    val userType: UserType,
    val provider: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
    val role: UserRole,
    val commentCount: Long,
    val primarySite: SiteInfo?, // 가장 많이 활동한 사이트
    val activeSites: List<SiteInfo> // 활동한 모든 사이트
)

// 사이트 정보
data class SiteInfo(
    val siteId: Long,
    val siteName: String?,
    val siteDomain: String,
    val commentCount: Long
)

// 사용자 목록 응답
data class UserListResponse(
    val users: List<UserWithSiteResponse>,
    val totalCount: Long,
    val currentPage: Int,
    val totalPages: Int
)

// 사용자 상세 응답
data class UserDetailResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
    val userType: UserType,
    val provider: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
    val role: UserRole,
    val totalCommentCount: Long,
    val activeSites: List<SiteActivityInfo>,
    val recentComments: List<RecentCommentInfo>
)

// 사이트별 활동 정보
data class SiteActivityInfo(
    val siteId: Long,
    val siteName: String?,
    val siteDomain: String,
    val commentCount: Long,
    val lastCommentAt: LocalDateTime?
)

// 최근 댓글 정보
data class RecentCommentInfo(
    val id: Long,
    val content: String,
    val siteId: Long,
    val siteName: String?,
    val siteDomain: String,
    val pageId: String,
    val createdAt: LocalDateTime
)

// 사용자 응답 (기본)
data class UserResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
    val userType: UserType,
    val provider: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
    val role: UserRole
)

// 인증 응답
data class AuthResponse(
    val token: String,
    val user: UserResponse,
    val expiresIn: Long
)

// 회원가입 요청
data class RegisterRequest(
    val email: String,
    val password: String,
    val nickname: String
)

// 로그인 요청
data class LoginRequest(
    val email: String,
    val password: String
)

// 게스트 댓글 요청
data class GuestCommentRequest(
    val nickname: String,
    val email: String?,
    val content: String,
    val pageId: String,
    val siteKey: String,
    val parentId: Long? = null
)
