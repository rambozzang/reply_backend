package com.comdeply.comment.app.admin.svc.vo

/**
 * 모든 SSO 세션 종료 응답 DTO
 */
data class SsoAllSessionsTerminationResponse(
    val siteId: Long,
    val terminatedCount: Int,
    val message: String,
    val terminatedAt: Long
)
