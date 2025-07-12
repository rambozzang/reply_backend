package com.comdeply.comment.app.admin.svc.vo

/**
 * 관리자 통계 응답 DTO (확장)
 */
data class AdminStatisticsResponse(
    val totalAdmins: Long,
    val activeAdmins: Long,
    val superAdmins: Long,
    val siteAdmins: Long,
    val moderators: Long,
    val recentLogins: Long,
    val inactiveAdmins: Long,
    val lastUpdated: Long
)
