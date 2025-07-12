package com.comdeply.comment.app.admin.svc.vo

/**
 * 댓글 통계 응답 DTO
 */
data class CommentStatsResponse(
    val totalComments: Long,
    val pendingComments: Long,
    val approvedComments: Long,
    val rejectedComments: Long
)
