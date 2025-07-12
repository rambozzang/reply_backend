package com.comdeply.comment.app.admin.svc.vo

import java.time.LocalDateTime

/**
 * 고급 통계 응답 DTO
 */
data class AdvancedStatsResponse(
    val message: String,
    val features: List<String>,
    val lastUpdated: LocalDateTime
)
