package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "sso_sessions",
    indexes = [
        Index(name = "idx_sso_session_token", columnList = "sessionToken"),
        Index(name = "idx_sso_session_expires", columnList = "expiresAt"),
        Index(name = "idx_sso_session_user", columnList = "ssoUserId")
    ]
)
data class SsoSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val ssoUserId: Long,

    // 세션 토큰 (JWT)
    @Column(nullable = false, unique = true, length = 500)
    val sessionToken: String,

    // IP 주소 및 User Agent (보안)
    @Column(length = 45)
    val ipAddress: String? = null,

    @Column(length = 500)
    val userAgent: String? = null,

    // 세션 만료 시간
    @Column(nullable = false)
    val expiresAt: LocalDateTime,

    // 세션 상태
    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    // 마지막 활동 시간
    @Column(nullable = false)
    val lastActivityAt: LocalDateTime = LocalDateTime.now(),

    // 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ssoUserId", insertable = false, updatable = false)
    val ssoUser: SsoUser? = null
)
