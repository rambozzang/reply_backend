package com.comdeply.comment.app.admin.svc.vo

import com.comdeply.comment.dto.AdminResponse
import com.comdeply.comment.entity.AdminSite

/**
 * 관리자 상세 응답 DTO
 */
data class AdminDetailResponse(
    val admin: AdminResponse,
    val sitePermissions: List<AdminSite>,
    val canManage: Boolean
)
