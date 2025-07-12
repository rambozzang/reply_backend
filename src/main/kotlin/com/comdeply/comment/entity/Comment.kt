package com.comdeply.comment.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "comments")
data class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val siteId: Long,

    @Column(nullable = true)
    val siteKey: String? = null,

    @Column(nullable = false)
    val pageId: String, // 고객 사이트의 페이지 식별자

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = true)
    val parentId: Long? = null, // 대댓글을 위한 부모 댓글 ID

    @Column(nullable = false)
    val depth: Int = 1, // 댓글 깊이 (1: 최상위 댓글, 2: 답글, 3: 답글의 답글...)

    @Column(nullable = false, precision = 10, scale = 3)
    val sortOrder: BigDecimal = BigDecimal.ZERO, // 정렬 순서 (소수점 기반 - 1.0, 1.1, 1.11...)

    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val isDeleted: Boolean = false,

    @Column(nullable = false)
    val isModerated: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: CommentStatus = CommentStatus.PENDING,

    @Column(nullable = false)
    val likeCount: Int = 0,

    @Column(nullable = false)
    val dislikeCount: Int = 0,

    // 사용자 정보 (비정규화 - 성능을 위해)
    @Column(nullable = false)
    val userNickname: String,

    @Column(nullable = true)
    val userProfileImageUrl: String? = null,

    // IP 주소 정보
    @Column(nullable = true, length = 45) // IPv6 지원을 위해 45자
    val ipAddress: String? = null,

    // 첨부파일 관계
    @OneToMany(mappedBy = "comment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val attachments: MutableList<CommentAttachment> = mutableListOf()
)
