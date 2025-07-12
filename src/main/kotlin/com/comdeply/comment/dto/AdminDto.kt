package com.comdeply.comment.dto

import com.comdeply.comment.entity.AdminRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// 관리자 로그인 요청
data class AdminLoginRequest(
    @field:NotBlank(message = "사용자명 또는 이메일은 필수입니다")
    val usernameOrEmail: String,
    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String
)

// 관리자 회원가입 요청
data class AdminRegisterRequest(
    @field:NotBlank(message = "사용자명은 필수입니다")
    @field:Size(min = 3, max = 50, message = "사용자명은 3-50자 사이여야 합니다")
    val username: String,
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8-100자 사이여야 합니다")
    val password: String,
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
    val name: String,
    val role: AdminRole = AdminRole.ADMIN
)

// 관리자 정보 수정 요청
data class AdminUpdateRequest(
    val name: String?,
    val email: String?,
    val profileImageUrl: String?,
    val isActive: Boolean?
)

// 관리자 비밀번호 변경 요청
data class AdminPasswordChangeRequest(
    @field:NotBlank(message = "현재 비밀번호는 필수입니다")
    val currentPassword: String,
    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    @field:Size(min = 8, max = 100, message = "새 비밀번호는 8-100자 사이여야 합니다")
    val newPassword: String
)

// 관리자 생성 요청 (SUPER_ADMIN 전용)
data class AdminCreateRequest(
    @field:NotBlank(message = "사용자명은 필수입니다")
    @field:Size(min = 3, max = 50, message = "사용자명은 3-50자 사이여야 합니다")
    val username: String,
    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 8, max = 100, message = "비밀번호는 8-100자 사이여야 합니다")
    val password: String,
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
    val name: String,
    val role: AdminRole = AdminRole.ADMIN
)

// 관리자 역할 변경 요청
data class AdminRoleChangeRequest(
    val role: AdminRole
)

// 관리자 응답
data class AdminResponse(
    val id: Long,
    val username: String,
    val email: String,
    val name: String,
    val role: AdminRole,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val lastLoginAt: LocalDateTime?,
    val profileImageUrl: String?
)

// 관리자 로그인 응답 (AuthResponse와 통일된 구조)
data class AdminAuthResponse(
    val token: String,
    val admin: AdminResponse,
    val expiresIn: Long
)

// 관리자 회원가입 응답
data class AdminRegisterResponse(
    val admin: AdminResponse,
    val isNewUser: Boolean = true
)

// 관리자 목록 응답 (Deprecated: PageResponse<AdminResponse> 사용)
@Deprecated("Use PageResponse<AdminResponse> instead")
data class AdminListResponse(
    val admins: List<AdminResponse>,
    val totalCount: Long
)

// 관리자 통계 응답
data class AdminStatsResponse(
    val totalAdmins: Long,
    val activeAdmins: Long,
    val superAdmins: Long,
    val siteAdmins: Long,
    val moderators: Long,
    val recentLogins: Long
)

// === 사이트 관리자 관련 DTO ===

// 사이트 관리자 할당 요청
data class AssignSiteAdminRequest(
    val adminId: Long,
    val siteId: Long,
    val permission: com.comdeply.comment.entity.SitePermission = com.comdeply.comment.entity.SitePermission.MANAGE
)

// 사이트 관리자 정보 응답
data class SiteAdminResponse(
    val id: Long,
    val adminId: Long,
    val siteId: Long,
    val permission: com.comdeply.comment.entity.SitePermission,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val assignedBy: Long?,
    // 관리자 정보
    val adminName: String,
    val adminEmail: String,
    val adminUsername: String,
    // 사이트 정보
    val siteName: String?,
    val siteDomain: String
)

// 관리자의 사이트 목록 응답
data class AdminSitesResponse(
    val adminId: Long,
    val adminName: String,
    val sites: List<SiteAdminResponse>
)

// 사이트의 관리자 목록 응답
data class SiteAdminsResponse(
    val siteId: Long,
    val siteName: String?,
    val siteDomain: String,
    val admins: List<SiteAdminResponse>
)

// 사이트 관리자 할당 목록 응답
data class SiteAdminAssignmentsResponse(
    val assignments: List<SiteAdminResponse>,
    val totalCount: Long
)
