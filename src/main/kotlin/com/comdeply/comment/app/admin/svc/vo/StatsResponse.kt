package com.comdeply.comment.app.admin.svc.vo

/**
 * 통계 응답 DTO
 */
data class StatsResponse(
    val totalComments: Long,
    val totalUsers: Long,
    val totalSites: Long,
    val todayComments: Long,
    val ssoUsers: Long,
    val activeSsoSessions: Long,
    val todaySsoLogins: Long,
    val ssoActiveSites: Long,
    val totalPayments: Long,
    val totalRevenue: Long,
    val paidPayments: Long
)
