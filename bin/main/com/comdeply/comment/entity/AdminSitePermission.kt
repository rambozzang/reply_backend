package com.comdeply.comment.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "admin_site_permissions")
data class AdminSitePermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "admin_id", nullable = false)
    val adminId: Long,

    @Column(name = "site_id", nullable = false)
    val siteId: String,

    @Column(name = "site_name")
    var siteName: String? = null,

    @Column(name = "site_description")
    var siteDescription: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val permission: SitePermissionType = SitePermissionType.MANAGE,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class SitePermissionType {
    OWNER,      // 사이트 소유자 (모든 권한)
    MANAGE,     // 관리자 (스킨, 댓글 관리)
    VIEW_ONLY   // 읽기 전용
}