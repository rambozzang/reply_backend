package com.comdeply.comment.app.admin.svc.vo

/**
 * 사이트 상태 변경 응답 DTO
 */
data class SiteStatusResponse(
    val siteId: Long,
    val isActive: Boolean,
    val message: String,
    val changedAt: Long
)
