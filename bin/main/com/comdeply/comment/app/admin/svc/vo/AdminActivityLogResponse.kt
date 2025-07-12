package com.comdeply.comment.app.admin.svc.vo

/**
 * 관리자 활동 로그 응답 DTO
 */
data class AdminActivityLogResponse(
    val logs: List<AdminActivityLog>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int
)
