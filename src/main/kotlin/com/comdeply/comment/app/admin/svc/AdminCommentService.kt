package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.app.admin.svc.vo.CommentStatsResponse
import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.Comment
import com.comdeply.comment.entity.CommentStatus
import com.comdeply.comment.repository.CommentLikeRepository
import com.comdeply.comment.repository.CommentRepository
import com.comdeply.comment.repository.SiteRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class AdminCommentService(
    private val commentRepository: CommentRepository,
    private val commentLikeRepository: CommentLikeRepository,
    private val siteRepository: SiteRepository,
    private val adminPermissionService: AdminPermissionService
) {
    private val logger = LoggerFactory.getLogger(AdminCommentService::class.java)

    /**
     * 관리자용 댓글 목록 조회 (권한에 따른 필터링 포함)
     */
    @Transactional(readOnly = true)
    fun getCommentsForAdmin(
        admin: Admin,
        siteId: Long?,
        pageId: String?,
        status: CommentStatus?,
        search: String?,
        page: Int,
        size: Int
    ): CommentListResponse {
        logger.info(
            "관리자 댓글 조회: adminId={}, siteId={}, pageId={}, status={}, search={}, page={}, size={}",
            admin.id,
            siteId,
            pageId,
            status,
            search,
            page,
            size
        )

        val pageable: Pageable = PageRequest.of(page, size)
        val accessibleSiteIds = adminPermissionService.getAccessibleSiteIds(admin)

        // 권한에 따른 사이트 필터링
        val targetSiteIds = when {
            // 특정 사이트 ID가 지정된 경우 - 권한 확인
            siteId != null -> {
                if (!adminPermissionService.hasPermissionForSite(admin, siteId) && !adminPermissionService.canViewGlobalStats(admin)) {
                    throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
                }
                listOf(siteId)
            }
            // SUPER_ADMIN의 경우 (accessibleSiteIds가 비어있음)
            accessibleSiteIds.isEmpty() -> emptyList() // 모든 사이트 조회
            // 일반 관리자의 경우
            else -> accessibleSiteIds
        }

        val commentsPage = when {
            // 모든 사이트 조회 (SUPER_ADMIN)
            targetSiteIds.isEmpty() -> {
                if (pageId != null) {
                    commentRepository.findByPageIdAndIsDeletedFalse(pageId, pageable)
                } else {
                    commentRepository.findByIsDeletedFalse(pageable)
                }
            }
            // 특정 사이트들만 조회
            else -> {
                if (pageId != null) {
                    commentRepository.findBySiteIdInAndPageIdAndIsDeletedFalse(targetSiteIds, pageId, pageable)
                } else {
                    commentRepository.findBySiteIdInAndIsDeletedFalse(targetSiteIds, pageable)
                }
            }
        }

        // 추가 필터링 (status, search)
        var filteredComments = commentsPage.content

        // 상태 필터링
        if (status != null) {
            filteredComments = filteredComments.filter { it.status == status }
        }

        // 검색 필터링 (댓글 내용 또는 사용자 닉네임)
        if (!search.isNullOrBlank()) {
            filteredComments = filteredComments.filter { comment ->
                comment.content.contains(search, ignoreCase = true) ||
                    comment.userNickname.contains(search, ignoreCase = true)
            }
        }

        val comments = filteredComments.map { comment ->
            convertToAdminCommentResponse(comment)
        }

        return CommentListResponse(
            comments = comments,
            totalCount = comments.size.toLong(), // 필터링 후 개수
            currentPage = page,
            totalPages = (comments.size + size - 1) / size, // 필터링 후 총 페이지 수
            hasNext = (page + 1) * size < comments.size
        )
    }

    /**
     * 관리자용 댓글 상태 업데이트
     */
    fun updateCommentStatus(
        commentId: Long,
        newStatus: CommentStatus,
        reason: String?,
        admin: Admin
    ): CommentResponse {
        logger.info(
            "댓글 상태 변경: commentId={}, newStatus={}, reason={}, adminId={}",
            commentId,
            newStatus,
            reason,
            admin.id
        )

        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId") }

        // 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, comment.siteId) && !adminPermissionService.canViewGlobalStats(admin)) {
            throw IllegalArgumentException("해당 댓글에 대한 접근 권한이 없습니다")
        }

        if (comment.isDeleted) {
            throw IllegalArgumentException("삭제된 댓글의 상태는 변경할 수 없습니다")
        }

        val updatedComment = comment.copy(
            status = newStatus,
            isModerated = newStatus != CommentStatus.PENDING,
            updatedAt = LocalDateTime.now()
        )

        val savedComment = commentRepository.save(updatedComment)

        logger.info(
            "댓글 상태 변경 완료: commentId={}, oldStatus={}, newStatus={}",
            commentId,
            comment.status,
            newStatus
        )

        return convertToAdminCommentResponse(savedComment)
    }

    /**
     * 관리자용 댓글 삭제 (소프트 삭제)
     */
    fun deleteCommentByAdmin(commentId: Long, admin: Admin): CommentResponse {
        logger.info("관리자 댓글 삭제: commentId={}, adminId={}", commentId, admin.id)

        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId") }

        // 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, comment.siteId) && !adminPermissionService.canViewGlobalStats(admin)) {
            throw IllegalArgumentException("해당 댓글에 대한 접근 권한이 없습니다")
        }

        if (comment.isDeleted) {
            throw IllegalArgumentException("이미 삭제된 댓글입니다")
        }

        // 자식 댓글이 있는지 확인
        val hasChildren = commentRepository.findByParentIdAndIsDeletedFalse(commentId).isNotEmpty()

        val deletedComment = if (hasChildren) {
            // 자식 댓글이 있으면 내용만 삭제하고 구조 유지
            comment.copy(
                content = "관리자에 의해 삭제된 댓글입니다.",
                isDeleted = true,
                updatedAt = LocalDateTime.now()
            )
        } else {
            // 자식 댓글이 없으면 완전 삭제 마킹
            comment.copy(
                isDeleted = true,
                updatedAt = LocalDateTime.now()
            )
        }

        val savedComment = commentRepository.save(deletedComment)

        // 자식이 없는 삭제된 댓글의 경우, 부모도 삭제된 상태이고 자식이 없다면 연쇄 정리
        if (!hasChildren) {
            cleanupDeletedCommentChain(savedComment)
        }

        logger.info("관리자 댓글 삭제 완료: commentId={}", commentId)

        return convertToAdminCommentResponse(savedComment)
    }

    /**
     * 관리자용 댓글 상세 조회
     */
    @Transactional(readOnly = true)
    fun getCommentDetail(commentId: Long, admin: Admin): CommentResponse {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId") }

        // 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, comment.siteId) && !adminPermissionService.canViewGlobalStats(admin)) {
            throw IllegalArgumentException("해당 댓글에 대한 접근 권한이 없습니다")
        }

        return convertToAdminCommentResponse(comment)
    }

    /**
     * 댓글 통계 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    fun getCommentStats(admin: Admin, siteId: Long?): CommentStatsResponse {
        val accessibleSiteIds = adminPermissionService.getAccessibleSiteIds(admin)

        val targetSiteIds = when {
            siteId != null -> {
                if (!adminPermissionService.hasPermissionForSite(admin, siteId) && !adminPermissionService.canViewGlobalStats(admin)) {
                    throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
                }
                listOf(siteId)
            }
            accessibleSiteIds.isEmpty() -> null // 모든 사이트
            else -> accessibleSiteIds
        }

        val totalComments = if (targetSiteIds == null) {
            commentRepository.count()
        } else {
            commentRepository.countBySiteIdIn(targetSiteIds)
        }

        val pendingComments = if (targetSiteIds == null) {
            commentRepository.countByStatus(CommentStatus.PENDING)
        } else {
            commentRepository.countBySiteIdInAndStatus(targetSiteIds, CommentStatus.PENDING)
        }

        val approvedComments = if (targetSiteIds == null) {
            commentRepository.countByStatus(CommentStatus.APPROVED)
        } else {
            commentRepository.countBySiteIdInAndStatus(targetSiteIds, CommentStatus.APPROVED)
        }

        val rejectedComments = if (targetSiteIds == null) {
            commentRepository.countByStatus(CommentStatus.REJECTED)
        } else {
            commentRepository.countBySiteIdInAndStatus(targetSiteIds, CommentStatus.REJECTED)
        }

        return CommentStatsResponse(
            totalComments = totalComments,
            pendingComments = pendingComments,
            approvedComments = approvedComments,
            rejectedComments = rejectedComments
        )
    }

    /**
     * 삭제된 댓글 체인 정리
     */
    @Transactional
    fun cleanupDeletedCommentChain(comment: Comment) {
        // 현재 댓글이 삭제되었고 자식이 없는 경우
        if (!comment.isDeleted) return

        val hasChildren = commentRepository.findByParentIdAndIsDeletedFalse(comment.id).isNotEmpty()
        if (hasChildren) return

        // 부모 댓글도 확인해서 연쇄 정리
        comment.parentId?.let { parentId ->
            val parent = commentRepository.findById(parentId).orElse(null)
            if (parent != null && parent.isDeleted) {
                val parentHasOtherChildren = commentRepository.findByParentIdAndIsDeletedFalse(parentId)
                    .any { it.id != comment.id }

                if (!parentHasOtherChildren) {
                    // 부모도 삭제되었고 다른 살아있는 자식이 없으면 부모도 정리
                    cleanupDeletedCommentChain(parent)
                }
            }
        }
    }

    /**
     * Comment 엔티티를 관리자용 CommentResponse DTO로 변환
     */
    private fun convertToAdminCommentResponse(comment: Comment): CommentResponse {
        val userLike = null // 관리자 조회시에는 좋아요 정보 불필요

        // 삭제된 댓글의 경우 내용과 사용자 정보 처리
        val (displayContent, displayNickname, displayProfileImage) = if (comment.isDeleted) {
            Triple("삭제된 댓글입니다.", "삭제된 사용자", null)
        } else {
            Triple(comment.content, comment.userNickname, comment.userProfileImageUrl)
        }

        return CommentResponse(
            id = comment.id,
            siteKey = comment.siteKey ?: "",
            pageId = comment.pageId,
            userId = comment.userId,
            parentId = comment.parentId,
            depth = comment.depth,
            sortOrder = comment.sortOrder,
            content = displayContent,
            createdAt = comment.createdAt,
            updatedAt = comment.updatedAt,
            isDeleted = comment.isDeleted,
            isModerated = comment.isModerated,
            likeCount = comment.likeCount,
            dislikeCount = comment.dislikeCount,
            userNickname = displayNickname,
            userProfileImageUrl = displayProfileImage,
            userLikeType = null, // 관리자 조회시에는 좋아요 정보 불필요
            attachments = if (comment.isDeleted) {
                // 삭제된 댓글의 첨부파일은 표시하지 않음
                emptyList()
            } else {
                comment.attachments.map { attachment ->
                    CommentAttachmentResponse(
                        id = attachment.id,
                        fileName = attachment.fileName,
                        fileUrl = attachment.fileUrl,
                        fileType = attachment.fileType,
                        fileSize = attachment.fileSize,
                        thumbnailUrl = attachment.thumbnailUrl
                    )
                }
            },
            children = emptyList()
        )
    }
}
