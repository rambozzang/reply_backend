package com.comdeply.comment.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "skins")
data class Skin(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var description: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: SkinType = SkinType.PRIVATE,

    @Column(name = "admin_id", nullable = true)
    val adminId: Long?, // 스킨 소유자 관리자 ID (BUILT_IN, SHARED는 null)

    @Column(name = "is_shared", nullable = false)
    var isShared: Boolean = false, // 공유 여부

    @Column(columnDefinition = "TEXT")
    var theme: String, // JSON으로 저장

    @Column(columnDefinition = "LONGTEXT")
    var styles: String?, // CSS 스타일

    @Column(name = "folder_name", nullable = false, unique = true)
    val folderName: String, // 파일 시스템에서 사용할 폴더명

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class SkinType {
    BUILT_IN, // 시스템 기본 제공 스킨
    SHARED, // 공유된 스킨 (개인이 만든 후 공유)
    PRIVATE // 개인 전용 스킨
}
