package com.comdeply.comment.app.admin.svc.vo

import java.time.LocalDateTime

/**
 * 트렌드 통계 응답 DTO
 */
data class TrendStatsResponse(
    val message: String,
    val trends: List<String>,
    val lastUpdated: LocalDateTime
)
