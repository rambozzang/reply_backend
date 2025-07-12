package com.comdeply.comment.app.admin.svc.vo
/**
 * 결제 통계 응답 DTO
 */
data class PaymentStatsResponse(
    val totalPayments: Long,
    val totalRevenue: Long,
    val paidPayments: Long,
    val pendingPayments: Long,
    val failedPayments: Long,
    val todayPayments: Long,
    val todayRevenue: Long
)
