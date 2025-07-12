package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "comment_likes")
data class CommentLike(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val commentId: Long,

    @Column(nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val likeType: LikeType, // LIKE: 좋아요, DISLIKE: 싫어요

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
