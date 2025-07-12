package com.comdeply.comment.portone.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "billing_keys")
data class BillingKey(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val adminId: Long, // Admin 엔티티와 연결

    @Column(nullable = false, unique = true)
    val billingKey: String, // PortOne에서 발급받은 빌링키

    @Column(nullable = false)
    val customerId: String, // PortOne 고객 ID

    @Column(nullable = true)
    val cardName: String?, // 카드 이름

    @Column(nullable = true)
    val cardNumber: String?, // 마스킹된 카드번호

    @Column(nullable = true)
    val cardType: String?, // 카드 타입

    @Column(nullable = true)
    val bank: String?, // 발급 은행

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: BillingKeyStatus = BillingKeyStatus.ACTIVE,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = true)
    val deletedAt: LocalDateTime? = null
)

enum class BillingKeyStatus {
    ACTIVE,    // 활성
    DELETED,   // 삭제됨
    EXPIRED    // 만료됨
}