package com.comdeply.comment.app.admin.svc.vo
/**
 * SSO 사용자 비활성화 응답 DTO
 */
data class SsoUserDeactivationResponse(
    val ssoUserId: Long,
    val username: String,
    val terminatedSessionCount: Int,
    val message: String,
    val deactivatedAt: Long
)
