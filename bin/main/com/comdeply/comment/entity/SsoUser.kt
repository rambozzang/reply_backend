package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "sso_users",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["siteId", "externalUserId"])
    ],
    indexes = [
        Index(name = "idx_sso_site_external", columnList = "siteId, externalUserId"),
        Index(name = "idx_sso_user", columnList = "userId"),
        Index(name = "idx_sso_last_login", columnList = "lastLoginAt")
    ]
)
data class SsoUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // 사이트 정보
    @Column(nullable = false)
    val siteId: Long,

    // ComDeply 내부 사용자 ID
    @Column(nullable = false)
    val userId: Long,

    // 고객 사이트의 사용자 ID (외부 시스템의 고유 식별자)
    @Column(nullable = false, length = 255)
    val externalUserId: String,

    // 고객 사이트에서 제공된 사용자 정보
    @Column(nullable = false, length = 100)
    val externalUserName: String,

    @Column(length = 255)
    val externalEmail: String? = null,

    @Column(length = 500)
    val externalProfileImageUrl: String? = null,

    // 추가 메타데이터 (JSON 형태)
    @Column(columnDefinition = "TEXT")
    val externalMetadata: String? = null,

    // SSO 설정
    @Column(nullable = false)
    val isActive: Boolean = true,

    // 타임스탬프
    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val lastLoginAt: LocalDateTime = LocalDateTime.now(),

    // 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siteId", insertable = false, updatable = false)
    val site: Site? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    val user: User? = null
)
