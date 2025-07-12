package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "subscriptions")
data class Subscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    val admin: Admin,

    @Column(name = "plan_id", nullable = false)
    var planId: String,

    @Column(name = "plan_name", nullable = false)
    var planName: String,

    @Column(name = "amount", nullable = false)
    var amount: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: SubscriptionStatus,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDateTime,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDateTime,

    @Column(name = "next_billing_date")
    var nextBillingDate: LocalDateTime? = null,

    @Column(name = "auto_renewal", nullable = false)
    var autoRenewal: Boolean = true,

    @Column(name = "monthly_comment_limit", nullable = false)
    var monthlyCommentLimit: Int,

    @Column(name = "current_comment_count", nullable = false)
    var currentCommentCount: Int = 0,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @Column(name = "cancel_reason")
    var cancelReason: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class SubscriptionStatus {
    ACTIVE, // 활성
    INACTIVE, // 비활성
    CANCELLED, // 취소됨
    EXPIRED // 만료됨
}
