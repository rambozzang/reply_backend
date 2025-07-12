package com.comdeply.comment.app.admin.svc.vo

/**
 * 관리자 활성화/비활성화 응답 DTO
 */
data class AdminActivationResponse(
    val adminId: Long,
    val username: String,
    val isActive: Boolean,
    val message: String,
    val changedAt: Long
)
