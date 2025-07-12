package com.comdeply.comment.app.web.svc

import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.Comment
import com.comdeply.comment.entity.CommentLike
import com.comdeply.comment.entity.LikeType
import com.comdeply.comment.entity.User
import com.comdeply.comment.repository.CommentLikeRepository
import com.comdeply.comment.repository.CommentRepository
import com.comdeply.comment.repository.SiteRepository
import com.comdeply.comment.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
@Transactional
class CommentService(
    private val commentRepository: CommentRepository,
    private val commentLikeRepository: CommentLikeRepository,
    private val userRepository: UserRepository,
    private val siteRepository: SiteRepository,
    private val subscriptionService: SubscriptionService
) {

    @Value("\${comment.max-depth}")
    private var maxDepth: Int = 3

    // 소수점 기반 sortOrder 시스템으로 sortIncrement 불필요

    /**
     * 댓글 목록 조회 (평면 구조)
     */
    fun getComments(siteKey: String, pageId: String, page: Int, size: Int, userId: Long?): CommentListResponse {
        val pageable: Pageable = PageRequest.of(page, size)
        val commentsPage = commentRepository.findBySiteKeyAndPageIdAndIsDeletedFalse(siteKey, pageId, pageable)

        val comments = commentsPage.content.map { comment ->
            toCommentResponse(comment, userId)
        }

        val totalCount = commentRepository.countBySiteKeyAndPageIdAndIsDeletedFalse(siteKey, pageId)

        return CommentListResponse(
            comments = comments,
            totalCount = totalCount,
            currentPage = page,
            totalPages = commentsPage.totalPages,
            hasNext = commentsPage.hasNext()
        )
    }

    /**
     * 댓글 목록 조회 (계층 구조)
     */
    fun getCommentsHierarchy(siteKey: String, pageId: String, page: Int, size: Int, userId: Long?): CommentListResponse {
        val pageable: Pageable = PageRequest.of(page, size)
        val parentCommentsPage = commentRepository.findParentCommentsBySiteKeyAndPageId(siteKey, pageId, pageable)

        // 해당 페이지의 모든 댓글을 조회하여 무한 깊이 지원
        val allComments = commentRepository.findBySiteKeyAndPageIdAndIsDeletedFalseOrderBySortOrder(siteKey, pageId)

        // 댓글을 ID로 그룹핑하여 빠른 조회 가능하도록 맵 생성
        val commentMap = allComments.associateBy { it.id }
        val childrenMap = allComments.filter { it.parentId != null }
            .groupBy { it.parentId!! }

        val comments = parentCommentsPage.content.map { parentComment ->
            buildCommentHierarchyOptimized(parentComment, childrenMap, commentMap, userId, 1)
        }

        val totalCount = commentRepository.countBySiteKeyAndPageIdAndIsDeletedFalse(siteKey, pageId)

        return CommentListResponse(
            comments = comments,
            totalCount = totalCount,
            currentPage = page,
            totalPages = parentCommentsPage.totalPages,
            hasNext = parentCommentsPage.hasNext()
        )
    }

    /**
     * 댓글 계층 구조 생성 (재귀 방식)
     */
    private fun buildCommentHierarchy(comment: Comment, userId: Long?, currentDepth: Int): CommentResponse {
        // 3단계 이후의 댓글들도 계층 구조로 표시 (무한 깊이)
        val children = commentRepository.findByParentIdIncludingDeletedWithChildren(comment.id)
            .map { child -> buildCommentHierarchy(child, userId, currentDepth + 1) }

        return toCommentResponse(comment, userId).copy(children = children)
    }

    /**
     * 댓글 계층 구조 생성 (최적화된 맵 기반)
     */
    private fun buildCommentHierarchyOptimized(
        comment: Comment,
        childrenMap: Map<Long, List<Comment>>,
        @Suppress("UNUSED_PARAMETER") commentMap: Map<Long, Comment>,
        userId: Long?,
        currentDepth: Int
    ): CommentResponse {
        // 3단계 이후의 댓글들도 계층 구조로 표시 (무한 깊이)
        val children = childrenMap[comment.id]?.sortedBy { it.sortOrder }?.map { child ->
            buildCommentHierarchyOptimized(child, childrenMap, commentMap, userId, currentDepth + 1)
        } ?: emptyList()

        return toCommentResponse(comment, userId).copy(children = children)
    }

    /**
     * 새 댓글 생성
     */
    fun createComment(request: CommentCreateRequest, userId: Long, ipAddress: String? = null): CommentResponse {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        val site = siteRepository.findBySiteKey(request.siteKey) ?: throw IllegalArgumentException("Site not found")

        if (!site.isActive) {
            throw IllegalArgumentException("Site is not active")
        }

        // 구독 댓글 한도 확인 (사이트 소유자 기준)
        val adminId = site.ownerId
        if (!subscriptionService.checkCommentLimit(adminId)) {
            throw IllegalStateException("월 댓글 한도를 초과했습니다. 플랜을 업그레이드해주세요.")
        }

        // 부모 댓글 검증 및 depth 계산
        val (parentComment, depth) = if (request.parentId != null) {
            val parent = commentRepository.findByIdAndNotDeleted(request.parentId)
                ?: throw IllegalArgumentException("Parent comment not found")

            // 3단계 댓글에 댓글을 달면 3단계로 유지하고 부모 댓글 바로 다음에 위치
            val newDepth = if (parent.depth >= maxDepth) maxDepth else parent.depth + 1

            parent to newDepth
        } else {
            null to 1
        }

        // sortOrder 계산 (소수점 기반)
        val sortOrder = calculateSortOrder(request.siteKey, request.pageId, request.parentId, parentComment)

        val comment = Comment(
            siteId = site.id,
            siteKey = request.siteKey,
            pageId = request.pageId,
            userId = userId,
            parentId = request.parentId,
            depth = depth,
            sortOrder = sortOrder,
            content = request.content,
            userNickname = user.nickname,
            userProfileImageUrl = user.profileImageUrl,
            ipAddress = ipAddress
        )

        // 소수점 기반에서는 중간 삽입 로직이 불필요 - 바로 저장
        val savedComment = commentRepository.save(comment)

        // 구독 댓글 수 증가 (사이트 소유자 기준)
        subscriptionService.incrementCommentCount(adminId)

        return toCommentResponse(savedComment, userId)
    }

    /**
     * 댓글 정렬 순서 계산 (소수점 기반)
     */
    private fun calculateSortOrder(siteKey: String, pageId: String, parentId: Long?, parentComment: Comment?): BigDecimal {
        return if (parentId == null) {
            // 최상위 댓글인 경우: 1.0, 2.0, 3.0...
            val maxSortOrder = commentRepository.findMaxSortOrderByParent(siteKey, pageId) ?: BigDecimal.ZERO
            maxSortOrder.add(BigDecimal.ONE)
        } else {
            // 답글인 경우: 소수점 기반 계산
            val parentSortOrder = parentComment!!.sortOrder
            val maxChildSortOrder = commentRepository.findMaxSortOrderByParentId(parentId!!)

            if (maxChildSortOrder == null) {
                // 첫 번째 자식: 부모가 1.000이면 1.100, 2.000이면 2.100
                parentSortOrder.add(BigDecimal("0.100"))
            } else {
                // 기존 자식들 중 마지막 + 0.010
                maxChildSortOrder.add(BigDecimal("0.010"))
            }
        }
    }

    /**
     * 댓글 재정렬 필요성 확인
     */
    private fun requiresReordering(siteKey: String, pageId: String, parentComment: Comment?, sortOrder: BigDecimal): Boolean {
        // 소수점 기반에서는 공간 부족 시 재정렬 필요성 체크
        return false // 소수점 기반에서는 일반적으로 재정렬 불필요
    }

    /**
     * 부모 댓글 이후 정렬 순서 증가 (소수점 기반에서 미사용)
     */
    @Transactional
    private fun incrementSortOrderAfterParent(siteKey: String, pageId: String, parentSortOrder: BigDecimal, newCommentSortOrder: BigDecimal) {
        // 소수점 기반에서는 중간 삽입이 필요 없음 - 항상 새로운 소수점 값으로 삽입 가능
        // 이 함수는 더 이상 사용되지 않음
    }

    /**
     * 댓글 정렬 순서 재정렬
     */
    @Transactional
    fun reorderComments(siteKey: String, pageId: String, parentComment: Comment?) {
        if (parentComment == null) {
            // 최상위 댓글들 재정렬 (1.0, 2.0, 3.0...)
            val parentComments = commentRepository.findParentCommentsBySiteKeyAndPageId(siteKey, pageId, PageRequest.of(0, Int.MAX_VALUE))
            parentComments.content.forEachIndexed { index, comment ->
                val newSortOrder = BigDecimal(index + 1)
                commentRepository.save(comment.copy(sortOrder = newSortOrder))
            }
        } else {
            // 특정 부모의 자식 댓글들 재정렬 (1.1, 1.2, 1.3...)
            val childComments = commentRepository.findByParentIdAndIsDeletedFalse(parentComment.id)
            childComments.forEachIndexed { index, comment ->
                val increment = BigDecimal("0.1").multiply(BigDecimal(index + 1))
                val newSortOrder = parentComment.sortOrder.add(increment)
                commentRepository.save(comment.copy(sortOrder = newSortOrder))
            }
        }
    }

    /**
     * 모든 댓글 재정렬 (계층 구조 유지)
     */
    @Transactional
    fun reorderAllComments(siteKey: String, pageId: String) {
        // 해당 페이지의 모든 댓글을 계층 구조 순서로 재정렬 (소수점 기반: 1.0, 1.1, 1.11...)
        val allComments = commentRepository.findBySiteKeyAndPageIdAndIsDeletedFalseOrderBySortOrder(siteKey, pageId)

        var currentSortOrder = BigDecimal.ONE

        fun reorderRecursively(comments: List<Comment>, parentId: Long?, depth: Int) {
            val filteredComments = comments.filter { it.parentId == parentId }
                .sortedBy { it.createdAt } // 생성 시간 순으로 정렬

            filteredComments.forEachIndexed { index, comment ->
                val newSortOrder = if (parentId == null) {
                    // 최상위 댓글: 1.0, 2.0, 3.0...
                    BigDecimal(index + 1)
                } else {
                    // 자식 댓글: 부모의 sortOrder + 0.1, 0.2, 0.3... (깊이에 따라 소수점 자릿수 증가)
                    val parent = comments.find { it.id == parentId }!!
                    val increment = BigDecimal("0.1").divide(BigDecimal.TEN.pow(depth - 1), 3, RoundingMode.HALF_UP)
                    parent.sortOrder.add(increment.multiply(BigDecimal(index + 1)))
                }

                commentRepository.save(comment.copy(sortOrder = newSortOrder))

                // 자식 댓글들도 재귀적으로 처리
                reorderRecursively(allComments, comment.id, depth + 1)
            }
        }

        reorderRecursively(allComments, null, 1)
    }

    /**
     * 댓글 내용 수정
     */
    fun updateComment(commentId: Long, request: CommentUpdateRequest, userId: Long): CommentResponse {
        val comment = commentRepository.findById(commentId).orElseThrow { IllegalArgumentException("Comment not found") }

        if (comment.userId != userId) {
            throw IllegalArgumentException("You can only edit your own comments")
        }

        if (comment.isDeleted) {
            throw IllegalArgumentException("Cannot edit deleted comment")
        }

        val updatedComment = comment.copy(
            content = request.content,
            updatedAt = LocalDateTime.now()
        )

        val savedComment = commentRepository.save(updatedComment)
        return toCommentResponse(savedComment, userId)
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     */
    fun deleteComment(commentId: Long, userId: Long): CommentResponse {
        val comment = commentRepository.findById(commentId).orElseThrow { IllegalArgumentException("Comment not found") }

        if (comment.userId != userId) {
            throw IllegalArgumentException("You can only delete your own comments")
        }

        if (comment.isDeleted) {
            throw IllegalArgumentException("Comment is already deleted")
        }

        // 자식 댓글이 있는지 확인
        val hasChildren = commentRepository.findByParentIdAndIsDeletedFalse(commentId).isNotEmpty()

        val deletedComment = if (hasChildren) {
            // 자식 댓글이 있으면 내용만 삭제하고 구조 유지
            comment.copy(
                content = "삭제된 댓글입니다.",
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

        return toCommentResponse(savedComment, userId)
    }

    /**
     * 삭제된 댓글 체인 정리
     */
    @Transactional
    private fun cleanupDeletedCommentChain(comment: Comment) {
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
     * 댓글 좋아요/취소
     */
    fun likeComment(commentId: Long, userId: Long): CommentResponse {
        val comment = commentRepository.findById(commentId).orElseThrow { IllegalArgumentException("Comment not found") }

        val existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId)

        if (existingLike != null) {
            if (existingLike.likeType == LikeType.LIKE) {
                // 이미 좋아요를 눌렀다면 취소
                commentLikeRepository.delete(existingLike)
                val updatedComment = comment.copy(likeCount = comment.likeCount - 1)
                commentRepository.save(updatedComment)
                return toCommentResponse(updatedComment, userId)
            } else {
                // 싫어요를 좋아요로 변경
                val updatedLike = existingLike.copy(likeType = LikeType.LIKE)
                commentLikeRepository.save(updatedLike)
                val updatedComment = comment.copy(
                    likeCount = comment.likeCount + 1,
                    dislikeCount = comment.dislikeCount - 1
                )
                commentRepository.save(updatedComment)
                return toCommentResponse(updatedComment, userId)
            }
        } else {
            // 새로운 좋아요
            val like = CommentLike(
                commentId = commentId,
                userId = userId,
                likeType = LikeType.LIKE
            )
            commentLikeRepository.save(like)
            val updatedComment = comment.copy(likeCount = comment.likeCount + 1)
            commentRepository.save(updatedComment)
            return toCommentResponse(updatedComment, userId)
        }
    }

    /**
     * 댓글 싫어요/취소
     */
    fun dislikeComment(commentId: Long, userId: Long): CommentResponse {
        val comment = commentRepository.findById(commentId).orElseThrow { IllegalArgumentException("Comment not found") }

        val existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId)

        if (existingLike != null) {
            if (existingLike.likeType == LikeType.DISLIKE) {
                // 이미 싫어요를 눌렀다면 취소
                commentLikeRepository.delete(existingLike)
                val updatedComment = comment.copy(dislikeCount = comment.dislikeCount - 1)
                commentRepository.save(updatedComment)
                return toCommentResponse(updatedComment, userId)
            } else {
                // 좋아요를 싫어요로 변경
                val updatedLike = existingLike.copy(likeType = LikeType.DISLIKE)
                commentLikeRepository.save(updatedLike)
                val updatedComment = comment.copy(
                    likeCount = comment.likeCount - 1,
                    dislikeCount = comment.dislikeCount + 1
                )
                commentRepository.save(updatedComment)
                return toCommentResponse(updatedComment, userId)
            }
        } else {
            // 새로운 싫어요
            val like = CommentLike(
                commentId = commentId,
                userId = userId,
                likeType = LikeType.DISLIKE
            )
            commentLikeRepository.save(like)
            val updatedComment = comment.copy(dislikeCount = comment.dislikeCount + 1)
            commentRepository.save(updatedComment)
            return toCommentResponse(updatedComment, userId)
        }
    }

    /**
     * 사용자가 작성한 댓글 목록 조회
     */
    fun getUserComments(userId: Long, page: Int, size: Int): CommentListResponse {
        val pageable: Pageable = PageRequest.of(page, size)
        val commentsPage = commentRepository.findByUserIdAndIsDeletedFalse(userId, pageable)

        val comments = commentsPage.content.map { comment ->
            toCommentResponse(comment, userId)
        }

        return CommentListResponse(
            comments = comments,
            totalCount = commentsPage.totalElements,
            currentPage = page,
            totalPages = commentsPage.totalPages,
            hasNext = commentsPage.hasNext()
        )
    }

    /**
     * Comment 엔티티를 CommentResponse DTO로 변환
     */
    private fun toCommentResponse(comment: Comment, userId: Long?): CommentResponse {
        val userLike = if (userId != null) {
            commentLikeRepository.findByCommentIdAndUserId(comment.id, userId)
        } else {
            null
        }

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
            userLikeType = userLike?.likeType,
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

    /**
     * 게스트 사용자 댓글 작성
     */
    fun createGuestComment(request: GuestCommentRequest, guestUser: User): CommentResponse {
        val site = siteRepository.findBySiteKey(request.siteKey) ?: throw IllegalArgumentException("Site not found")

        if (!site.isActive) {
            throw IllegalArgumentException("Site is not active")
        }

        // 부모 댓글 검증 및 depth 계산
        val (parentComment, depth) = if (request.parentId != null) {
            val parent = commentRepository.findByIdAndNotDeleted(request.parentId)
                ?: throw IllegalArgumentException("Parent comment not found")

            // 3단계 댓글에 댓글을 달면 3단계로 유지하고 부모 댓글 바로 다음에 위치
            val newDepth = if (parent.depth >= maxDepth) maxDepth else parent.depth + 1

            parent to newDepth
        } else {
            null to 1
        }

        // sortOrder 계산 (소수점 기반)
        val sortOrder = calculateSortOrder(request.siteKey, request.pageId, request.parentId, parentComment)

        val comment = Comment(
            siteId = site.id,
            siteKey = request.siteKey,
            pageId = request.pageId,
            parentId = request.parentId,
            content = request.content,
            userId = guestUser.id,
            depth = depth,
            sortOrder = sortOrder,
            userNickname = guestUser.nickname
        )

        val savedComment = commentRepository.save(comment)

        // 첨부파일 처리 (필요시)
        // attachmentService.processAttachments(savedComment, request.attachments)

        return toCommentResponse(savedComment, guestUser.id)
    }

    /**
     * 페이지별 댓글 수 조회 (캐시 적용)
     */
    @Cacheable(value = ["commentCount"], key = "#siteKey + '_' + #pageId")
    fun getCommentCount(siteKey: String, pageId: String): Long {
        return commentRepository.countBySiteKeyAndPageIdAndIsDeletedFalse(siteKey, pageId)
    }

    /**
     * 여러 페이지의 댓글 수 일괄 조회 (캐시 적용)
     */
    @Cacheable(value = ["batchCommentCount"], key = "#siteKey + '_' + #pageIds.hashCode()")
    fun getBatchCommentCount(siteKey: String, pageIds: List<String>): Map<String, Long> {
        return commentRepository.countCommentsByPageIds(siteKey, pageIds)
            .associate { it.pageId to it.count }
    }
}
