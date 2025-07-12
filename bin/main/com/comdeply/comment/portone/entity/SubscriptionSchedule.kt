package com.comdeply.comment.portone.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "subscription_schedules")
data class SubscriptionSchedule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val adminId: Long, // Admin 엔티티와 연결

    @Column(nullable = false, unique = true)
    val scheduleId: String, // PortOne 스케줄 ID

    val subscriptionId: Long? = null, // 구독 ID
    
    val billingKeyId: Long? = null, // 빌링키 ID

    @Column(nullable = false)
    val planType: String, // PRO, PREMIUM, ENTERPRISE

    @Column(nullable = false, precision = 10, scale = 2)
    val amount: BigDecimal, // 결제 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val billingCycle: BillingCycle = BillingCycle.MONTHLY, // 결제 주기

    @Column(nullable = false)
    val nextPaymentDate: LocalDateTime, // 다음 결제일

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ScheduleStatus = ScheduleStatus.SCHEDULED,

    val lastPaymentDate: LocalDateTime? = null, // 마지막 결제일

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true)
    val canceledAt: LocalDateTime? = null,

    @Column(nullable = true)
    val cancelReason: String? = null
)

enum class ScheduleStatus {
    SCHEDULED,  // 예약됨
    ACTIVE,     // 활성 (결제 진행중)
    PAUSED,     // 일시정지
    SUSPENDED,  // 중단됨 (결제 실패 등)
    CANCELED,   // 취소됨
    COMPLETED   // 완료됨
}

enum class BillingCycle {
    MONTHLY,    // 월 결제
    YEARLY      // 연 결제
}