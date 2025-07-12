package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "sites")
data class Site(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val ownerId: Long,

    @Column(nullable = false)
    val siteName: String,

    @Column(nullable = false)
    val domain: String,

    @Column(nullable = false)
    val siteKey: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val isActive: Boolean = true,

    // 설정 정보
    @Column(columnDefinition = "TEXT")
    val themeColor: String? = "#007bff",

    @Column(columnDefinition = "TEXT")
    val customCss: String? = null,

    @Column(nullable = false)
    val requireAuth: Boolean = false,

    @Column(nullable = false)
    val enableModeration: Boolean = true,

    @Column(nullable = false)
    val theme: String = "light",

    @Column(nullable = false)
    val language: String = "ko",

    // 허용 도메인 목록 (JSON 형태로 저장)
    @Column(columnDefinition = "TEXT")
    val allowedDomains: String? = null,

    // SSO 설정
    @Column(nullable = false)
    val ssoEnabled: Boolean = false,

    @Column(length = 500)
    val ssoSecretKey: String? = null,

    @Column
    val ssoTokenExpirationMinutes: Int? = 60,

    @Column(columnDefinition = "TEXT")
    val ssoAllowedDomains: String? = null
)
