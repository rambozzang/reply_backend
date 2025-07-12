package com.comdeply.comment.app.admin.svc.vo

import java.time.LocalDateTime

/**
 * 상세 통계 응답 DTO
 */
data class DetailedStatsResponse(
    val summary: StatsResponse,
    val siteStats: List<SiteStatsItem>,
    val weeklyComments: List<WeeklyCommentsItem>,
    val lastUpdated: LocalDateTime
)
