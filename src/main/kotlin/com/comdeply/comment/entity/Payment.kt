package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "payments")
data class Payment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    val admin: Admin,

    @Column(name = "payment_id", unique = true, nullable = false)
    val paymentId: String,

    @Column(name = "plan_id", nullable = false)
    val planId: String,

    @Column(name = "plan_name", nullable = false)
    val planName: String,

    @Column(name = "amount", nullable = false)
    val amount: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: PaymentStatus,

    @Column(name = "paid_at")
    val paidAt: LocalDateTime? = null,

    @Column(name = "cancelled_at")
    val cancelledAt: LocalDateTime? = null,

    @Column(name = "refunded_at")
    val refundedAt: LocalDateTime? = null,

    @Column(name = "portone_transaction_id")
    val portoneTransactionId: String? = null,

    @Column(name = "payment_method")
    val paymentMethod: String? = null,

    @Column(name = "card_company")
    val cardCompany: String? = null,

    @Column(name = "card_number")
    val cardNumber: String? = null,

    @Column(name = "failure_reason")
    val failureReason: String? = null,

    @Column(name = "cancel_reason")
    val cancelReason: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class PaymentStatus {
    PENDING, // 결제 대기
    PAID, // 결제 완료
    FAILED, // 결제 실패
    CANCELLED, // 결제 취소
    REFUNDED // 환불 완료
}
