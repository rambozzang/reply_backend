package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "admin_sites",
    uniqueConstraints = [UniqueConstraint(columnNames = ["admin_id", "site_id"])]
)
data class AdminSite(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "admin_id", nullable = false)
    val adminId: Long,

    @Column(name = "site_id", nullable = false)
    val siteId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val permission: SitePermission = SitePermission.MANAGE,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column
    val assignedBy: Long? = null // 누가 할당했는지
)

enum class SitePermission {
    MANAGE, // 사이트 관리 권한 (댓글, 사용자, 설정 등)
    READ_ONLY, // 읽기 전용 권한 (통계 조회만)
    MODERATE // 모더레이션 권한 (댓글 승인/거절만)
}
