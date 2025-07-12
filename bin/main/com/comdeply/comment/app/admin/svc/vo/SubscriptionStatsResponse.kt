package com.comdeply.comment.app.admin.svc.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "구독 통계 응답")
data class SubscriptionStatsResponse(
    @Schema(description = "총 구독 수", example = "150")
    val totalSubscriptions: Long,

    @Schema(description = "활성 구독 수", example = "120")
    val activeSubscriptions: Long,

    @Schema(description = "취소된 구독 수", example = "30")
    val cancelledSubscriptions: Long,

    @Schema(description = "월 매출", example = "3480000")
    val monthlyRevenue: Long,

    @Schema(description = "유료 구독 수", example = "120")
    val paidSubscriptions: Long,

    @Schema(description = "통계 기간", example = "2024-01-01 ~ 2024-01-31")
    val period: String
)
