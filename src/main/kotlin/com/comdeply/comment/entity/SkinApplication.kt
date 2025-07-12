package com.comdeply.comment.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "skin_applications")
data class SkinApplication(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "site_id", nullable = false)
    var siteId: String,

    @Column(name = "page_id")
    var pageId: String?, // null이면 전체 사이트에 적용

    @Column(name = "skin_name", nullable = false)
    var skinName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var scope: ApplicationScope = ApplicationScope.SPECIFIC,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class ApplicationScope {
    ALL, // 전체 사이트
    SPECIFIC // 특정 페이지
}

// 확장 함수로 toResponseDto 추가
// fun SkinApplication.toResponseDto() =  SkinApplyResponse(
//    id = this.id,
//    siteKey = this.siteId,
//    pageId = this.pageId,
//    skinName = this.skinName,
//    scope = this.scope,
//    createdAt = this.createdAt,
//    updatedAt = this.updatedAt
// )
