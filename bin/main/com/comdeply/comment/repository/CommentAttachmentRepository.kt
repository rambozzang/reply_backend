package com.comdeply.comment.repository

import com.comdeply.comment.entity.CommentAttachment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentAttachmentRepository : JpaRepository<CommentAttachment, Long> {
    fun findByCommentId(commentId: Long): List<CommentAttachment>
    fun deleteByCommentId(commentId: Long)
}
