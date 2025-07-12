package com.comdeply.comment.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "댓글 생성 요청")
data class CommentCreateRequest(
    @Schema(description = "사이트 키", example = "abc123")
    val siteKey: String,

    @Schema(description = "페이지 ID", example = "/blog/post1")
    val pageId: String,

    @Schema(description = "부모 댓글 ID (답글인 경우)", example = "null")
    val parentId: Long?,

    @Schema(description = "댓글 내용", example = "좋은 글이네요!")
    val content: String,

    @Schema(description = "첨부파일 경로 목록", example = "[\"2024/01/01/image1.jpg\", \"2024/01/01/image2.jpg\"]")
    val attachments: List<String> = emptyList()
)

@Schema(description = "댓글 수정 요청")
data class CommentUpdateRequest(
    @Schema(description = "수정할 댓글 내용", example = "수정된 댓글 내용입니다")
    val content: String
)

@Schema(description = "댓글 응답")
data class CommentResponse(
    @Schema(description = "댓글 ID", example = "1")
    val id: Long,

    @Schema(description = "사이트 키", example = "abc123")
    val siteKey: String,

    @Schema(description = "페이지 ID", example = "/blog/post1")
    val pageId: String,

    @Schema(description = "부모 댓글 ID", example = "null")
    val parentId: Long?,

    @Schema(description = "댓글 깊이", example = "1")
    val depth: Int,

    @Schema(description = "정렬 순서", example = "1.0")
    val sortOrder: BigDecimal,

    @Schema(description = "댓글 내용", example = "좋은 글이네요!")
    val content: String,

    @Schema(description = "생성일시", example = "2024-01-01T10:00:00")
    val createdAt: LocalDateTime,

    @Schema(description = "수정일시", example = "2024-01-01T10:30:00")
    val updatedAt: LocalDateTime,

    @Schema(description = "좋아요 수", example = "5")
    val likeCount: Int,

    @Schema(description = "싫어요 수", example = "1")
    val dislikeCount: Int,

    @Schema(description = "작성자 닉네임", example = "홍길동")
    val userNickname: String,

    @Schema(description = "작성자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
    val userProfileImageUrl: String?,

    @Schema(description = "삭제 여부", example = "false")
    val isDeleted: Boolean,

    @Schema(description = "검열 여부", example = "false") val isModerated: Boolean,

    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,

    @Schema(description = "사용자 좋아요/싫어요 상태", example = "LIKE")
    val userLikeType: com.comdeply.comment.entity.LikeType?,

    @Schema(description = "첨부파일 목록")
    val attachments: List<CommentAttachmentResponse>,

    @Schema(description = "답글 목록")
    val children: List<CommentResponse> = emptyList()
) {
    companion object {
        fun from(comment: com.comdeply.comment.entity.Comment, user: com.comdeply.comment.entity.User): CommentResponse {
            return CommentResponse(
                id = comment.id,
                siteKey = comment.siteKey ?: "",
                pageId = comment.pageId,
                parentId = comment.parentId,
                depth = comment.depth,
                sortOrder = comment.sortOrder,
                content = comment.content,
                createdAt = comment.createdAt,
                updatedAt = comment.updatedAt,
                likeCount = 0, // TODO: calculate from likes
                dislikeCount = 0, // TODO: calculate from dislikes
                userNickname = user.nickname,
                userProfileImageUrl = user.profileImageUrl,
                isDeleted = comment.isDeleted,
                isModerated = comment.isModerated,
                userId = comment.userId,
                userLikeType = null, // TODO: get user's like status
                attachments = emptyList(), // TODO: load attachments
                children = emptyList()
            )
        }
    }
}

@Schema(description = "댓글 목록 응답 (Deprecated: PageResponse<CommentResponse> 사용)")
@Deprecated("Use PageResponse<CommentResponse> instead")
data class CommentListResponse(
    @Schema(description = "댓글 목록")
    val comments: List<CommentResponse>,

    @Schema(description = "전체 댓글 수", example = "100")
    val totalCount: Long,

    @Schema(description = "현재 페이지", example = "0")
    val currentPage: Int,

    @Schema(description = "전체 페이지 수", example = "5")
    val totalPages: Int,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean
)

@Schema(description = "댓글 첨부파일")
data class CommentAttachmentResponse(
    @Schema(description = "첨부파일 ID", example = "1")
    val id: Long,

    @Schema(description = "파일명", example = "image.jpg")
    val fileName: String,

    @Schema(description = "파일 타입", example = "IMAGE")
    val fileType: com.comdeply.comment.entity.FileType,

    @Schema(description = "파일 크기", example = "1024000")
    val fileSize: Long,

    @Schema(description = "파일 URL", example = "https://files.yourdomain.com/2024/01/01/image.jpg")
    val fileUrl: String,

    @Schema(description = "썸네일 URL", example = "https://files.yourdomain.com/2024/01/01/thumb_image.jpg")
    val thumbnailUrl: String?
)

@Schema(description = "댓글 수 응답")
data class CommentCountResponse(
    @Schema(description = "댓글 수", example = "42")
    val count: Long
)

@Schema(description = "배치 댓글 수 조회 요청")
data class BatchCommentCountRequest(
    @Schema(description = "사이트 키", example = "abc123")
    val siteKey: String,

    @Schema(description = "페이지 ID 목록", example = "[\"/blog/post1\", \"/blog/post2\", \"/blog/post3\"]")
    val pageIds: List<String>
)

@Schema(description = "배치 댓글 수 조회 응답")
data class BatchCommentCountResponse(
    @Schema(description = "페이지별 댓글 수", example = "{\"/blog/post1\": 15, \"/blog/post2\": 8, \"/blog/post3\": 23}")
    val counts: Map<String, Long>
)

data class PageCommentCount(
    val pageId: String,
    val count: Long
)
