package com.comdeply.comment.app.admin.svc.vo

/**
 * 사이트 삭제 응답 DTO
 */
data class SiteDeletionResponse(
    val siteId: Long,
    val message: String,
    val deletedAt: Long
)
