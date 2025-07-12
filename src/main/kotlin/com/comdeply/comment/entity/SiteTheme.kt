package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "site_themes",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["siteId", "pageId"])
    ]
)
data class SiteTheme(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val siteId: Long,

    @Column(nullable = false)
    val themeId: Long,

    // 페이지별 테마 관리를 위한 page_id 추가
    @Column(nullable = false)
    val pageId: String,

    // 커스터마이징된 설정 (JSON 형태)
    @Column(columnDefinition = "TEXT")
    val customizations: String? = null,

    // 활성화 여부
    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val appliedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    // 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siteId", insertable = false, updatable = false)
    val site: Site? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "themeId", insertable = false, updatable = false)
    val theme: Theme? = null
)
