package com.comdeply.comment.app.admin.svc.vo

/**
 * 관리자 활동 로그 DTO
 */
data class AdminActivityLog(
    val id: Long,
    val adminId: Long,
    val username: String,
    val action: String,
    val details: String,
    val ipAddress: String?,
    val timestamp: Long
)
