package com.comdeply.comment.app.admin.svc.vo

/**
 * SSO 세션 종료 응답 DTO
 */
data class SsoSessionTerminationResponse(
    val sessionId: Long,
    val ssoUserId: Long,
    val username: String,
    val message: String,
    val terminatedAt: Long
)
