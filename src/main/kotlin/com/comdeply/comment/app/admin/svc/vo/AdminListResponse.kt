package com.comdeply.comment.app.admin.svc.vo

import com.comdeply.comment.dto.AdminResponse

/**
 * 관리자 목록 응답 DTO
 */
data class AdminListResponse(
    val admins: List<AdminResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int
)
