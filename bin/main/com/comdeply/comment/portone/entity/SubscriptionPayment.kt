package com.comdeply.comment.portone.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "subscription_payments")
data class SubscriptionPayment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val adminId: Long, // Admin 엔티티와 연결

    @Column(nullable = false, unique = true)
    val paymentId: String, // PortOne 결제 ID
    
    val impUid: String? = null, // PortOne imp_uid

    val scheduleId: String? = null, // 스케줄 ID

    @Column(nullable = false, precision = 10, scale = 2)
    val amount: BigDecimal, // 결제 금액

    @Column(nullable = false)
    val currency: String = "KRW",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: PaymentStatus = PaymentStatus.PENDING,

    val description: String? = null, // 결제 설명
    
    val planType: String? = null, // 플랜 타입
    
    val failureReason: String? = null, // 실패 사유

    @Column(nullable = false)
    val paymentDate: LocalDateTime, // 결제 시도일

    @Column(nullable = true)
    val paidAt: LocalDateTime?, // 결제 완료일

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class PaymentStatus {
    PENDING,    // 결제 대기
    PAID,       // 결제 완료
    FAILED,     // 결제 실패
    CANCELED,   // 결제 취소
    PARTIAL_CANCELED // 부분 취소
}