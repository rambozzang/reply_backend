package com.comdeply.comment.app.admin.svc.vo

import com.comdeply.comment.entity.PaymentStatus
import java.time.LocalDateTime

/**
 * 결제 상세 응답 DTO
 */
data class PaymentDetailResponse(
    val id: Long,
    val userId: Long,
    val paymentId: String,
    val planId: String,
    val planName: String,
    val amount: Int,
    val status: PaymentStatus,
    val paymentMethod: String?,
    val portoneTransactionId: String?,
    val cardCompany: String?,
    val cardNumber: String?,
    val failureReason: String?,
    val cancelReason: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val paidAt: LocalDateTime?,
    val cancelledAt: LocalDateTime?,
    val refundedAt: LocalDateTime?
)
