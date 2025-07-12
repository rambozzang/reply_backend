package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "admins")
data class Admin(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false, length = 50)
    val username: String,

    @Column(nullable = false, length = 255)
    val password: String,

    @Column(unique = true, nullable = false, length = 100)
    val email: String,

    @Column(nullable = false, length = 50)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: AdminRole = AdminRole.ADMIN,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val lastLoginAt: LocalDateTime? = null,

    @Column(length = 500)
    val profileImageUrl: String? = null,

    // 구독 관계 (양방향)
    @OneToMany(mappedBy = "admin", cascade = [CascadeType.ALL], orphanRemoval = true)
    val subscriptions: MutableList<Subscription> = mutableListOf(),

    // 결제 관계 (양방향)
    @OneToMany(mappedBy = "admin", cascade = [CascadeType.ALL], orphanRemoval = true)
    val payments: MutableList<Payment> = mutableListOf()
)

enum class AdminRole {
    SUPER_ADMIN, // 슈퍼 관리자 - 모든 권한
    ADMIN, // 사이트 관리자 - 할당된 사이트만 관리 (기존 호환성을 위해 ADMIN 유지)
    MODERATOR // 모더레이터 - 제한적 권한
}
