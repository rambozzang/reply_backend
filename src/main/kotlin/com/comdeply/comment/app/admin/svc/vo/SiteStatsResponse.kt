package com.comdeply.comment.app.admin.svc.vo

import com.comdeply.comment.dto.PageStatsResponse

/**
 * 사이트 통계 응답 DTO
 */
data class SiteStatsResponse(
    val siteId: Long,
    val totalComments: Long,
    val pendingComments: Long,
    val approvedComments: Long,
    val rejectedComments: Long,
    val totalPages: Long,
    val activePages: Long,
    val totalUsers: Long,
    val activeUsers: Long,
    val lastUpdated: Long
)
data class SiteStatsResponse2(
    val siteId: Long,
    val totalComments: Long,
    val totalUsers: Long,
    val todayComments: Long,
    val popularPages: List<PageStatsResponse>
)
