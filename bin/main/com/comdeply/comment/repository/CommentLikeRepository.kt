package com.comdeply.comment.repository

import com.comdeply.comment.entity.CommentLike
import com.comdeply.comment.entity.LikeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentLikeRepository : JpaRepository<CommentLike, Long> {
    fun findByCommentIdAndUserId(commentId: Long, userId: Long): CommentLike?
    fun existsByCommentIdAndUserId(commentId: Long, userId: Long): Boolean
    fun countByCommentIdAndLikeType(commentId: Long, likeType: LikeType): Long
}
