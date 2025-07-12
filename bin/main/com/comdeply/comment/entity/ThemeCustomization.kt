package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "theme_customizations")
data class ThemeCustomization(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val siteId: Long,

    @Column(nullable = false)
    val themeId: Long,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 100)
    val name: String, // 커스터마이징 이름

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    // 커스터마이징된 색상
    @Column(columnDefinition = "TEXT")
    val customColors: String? = null,

    // 커스터마이징된 타이포그래피
    @Column(columnDefinition = "TEXT")
    val customTypography: String? = null,

    // 커스터마이징된 간격
    @Column(columnDefinition = "TEXT")
    val customSpacing: String? = null,

    // 커스터마이징된 테두리 반경
    @Column(columnDefinition = "TEXT")
    val customBorderRadius: String? = null,

    // 커스터마이징된 컴포넌트 설정
    @Column(columnDefinition = "TEXT")
    val customComponents: String? = null,

    // 추가 CSS
    @Column(columnDefinition = "TEXT")
    val additionalCss: String? = null,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    // 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "siteId", insertable = false, updatable = false)
    val site: Site? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "themeId", insertable = false, updatable = false)
    val theme: Theme? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    val user: User? = null
)
