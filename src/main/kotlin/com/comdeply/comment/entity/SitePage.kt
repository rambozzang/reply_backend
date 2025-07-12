package com.comdeply.comment.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "site_pages")
data class SitePage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "site_id", nullable = false)
    val siteId: String,

    @Column(name = "page_id", nullable = false)
    val pageId: String,

    @Column(name = "page_name")
    var pageName: String? = null,

    @Column(name = "page_description")
    var pageDescription: String? = null,

    @Column(name = "page_url")
    var pageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var pageType: PageType = PageType.BOARD,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Column(name = "comment_count")
    var commentCount: Long = 0,

    @Column(name = "last_activity_at")
    var lastActivityAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class PageType {
    BOARD,      // 게시판
    ARTICLE,    // 개별 글
    PRODUCT,    // 상품 페이지
    GENERAL,    // 일반 페이지
    OTHER       // 기타
}