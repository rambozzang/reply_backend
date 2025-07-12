package com.comdeply.comment.app.admin.svc.vo

import java.time.LocalDateTime

/**
 * 성능 통계 응답 DTO
 */
data class PerformanceStatsResponse(
    val message: String,
    val metrics: List<String>,
    val lastUpdated: LocalDateTime
)
