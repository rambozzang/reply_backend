package com.comdeply.comment.app.admin.svc.vo

/**
 * 사이트 통계 아이템 DTO
 */
data class SiteStatsItem(
    val id: Long,
    val name: String,
    val domain: String,
    val commentCount: Long
)
