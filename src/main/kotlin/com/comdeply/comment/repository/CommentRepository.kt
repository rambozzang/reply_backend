package com.comdeply.comment.repository

import com.comdeply.comment.entity.Comment
import com.comdeply.comment.entity.CommentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c WHERE c.siteKey = :siteKey AND c.pageId = :pageId AND c.isDeleted = false ORDER BY c.createdAt ASC")
    fun findBySiteKeyAndPageIdAndIsDeletedFalse(
        siteKey: String,
        pageId: String,
        pageable: Pageable
    ): Page<Comment>

    @Query(
        "SELECT c FROM Comment c WHERE c.siteId = :siteId AND c.pageId = :pageId AND (c.isDeleted = false OR (c.isDeleted = true AND EXISTS (SELECT 1 FROM Comment child WHERE child.parentId = c.id AND child.isDeleted = false))) ORDER BY c.sortOrder ASC"
    )
    fun findBySiteIdAndPageIdWithChildren(
        siteId: Long,
        pageId: String,
        pageable: Pageable
    ): Page<Comment>

    @Query(
        "SELECT c FROM Comment c WHERE c.siteKey = :siteKey AND c.pageId = :pageId AND c.parentId IS NULL AND (c.isDeleted = false OR (c.isDeleted = true AND EXISTS (SELECT 1 FROM Comment child WHERE child.parentId = c.id AND child.isDeleted = false))) ORDER BY c.sortOrder ASC"
    )
    fun findParentCommentsBySiteKeyAndPageId(
        siteKey: String,
        pageId: String,
        pageable: Pageable
    ): Page<Comment>

    @Query(
        "SELECT c FROM Comment c WHERE c.parentId = :parentId AND (c.isDeleted = false OR (c.isDeleted = true AND EXISTS (SELECT 1 FROM Comment child WHERE child.parentId = c.id AND child.isDeleted = false))) ORDER BY c.sortOrder ASC"
    )
    fun findByParentIdIncludingDeletedWithChildren(parentId: Long): List<Comment>

    @Query("SELECT c FROM Comment c WHERE c.parentId = :parentId AND c.isDeleted = false ORDER BY c.sortOrder ASC")
    fun findByParentIdAndIsDeletedFalse(parentId: Long): List<Comment>

    @Query("SELECT MAX(c.sortOrder) FROM Comment c WHERE c.siteKey = :siteKey AND c.pageId = :pageId AND c.parentId IS NULL")
    fun findMaxSortOrderByParent(
        siteKey: String,
        pageId: String
    ): BigDecimal?

    @Query("SELECT MAX(c.sortOrder) FROM Comment c WHERE c.siteId = :siteId AND c.pageId = :pageId AND c.parentId IS NULL")
    fun findMaxSortOrderByParentIdIsNull(
        siteId: Long,
        pageId: String
    ): BigDecimal?

    @Query("SELECT MAX(c.sortOrder) FROM Comment c WHERE c.parentId = :parentId")
    fun findMaxSortOrderByParentId(parentId: Long): BigDecimal?

    @Query("SELECT c FROM Comment c WHERE c.id = :id AND c.isDeleted = false")
    fun findByIdAndNotDeleted(id: Long): Comment?

    fun countBySiteKeyAndPageIdAndIsDeletedFalse(
        siteKey: String,
        pageId: String
    ): Long

    fun findByUserIdAndIsDeletedFalse(
        userId: Long,
        pageable: Pageable
    ): Page<Comment>

    @Query(
        """
        SELECT new com.comdeply.comment.dto.PageCommentCount(c.pageId, COUNT(c.id)) 
        FROM Comment c 
        WHERE c.siteKey = :siteKey 
        AND c.pageId IN :pageIds 
        AND c.isDeleted = false 
        GROUP BY c.pageId
    """
    )
    fun countCommentsByPageIds(
        siteKey: String,
        pageIds: List<String>
    ): List<com.comdeply.comment.dto.PageCommentCount>

    @Query(
        """
        SELECT c FROM Comment c 
        WHERE c.siteId = :siteId 
        AND c.pageId = :pageId 
        AND (c.id IN :parentIds 
             OR c.parentId IN :parentIds 
             OR EXISTS (SELECT 1 FROM Comment p WHERE p.id = c.parentId AND p.parentId IN :parentIds)
             OR EXISTS (SELECT 1 FROM Comment p2 WHERE p2.id = c.parentId 
                        AND EXISTS (SELECT 1 FROM Comment p3 WHERE p3.id = p2.parentId AND p3.parentId IN :parentIds))
        )
        AND (c.isDeleted = false OR (c.isDeleted = true AND EXISTS (
            SELECT 1 FROM Comment child WHERE child.parentId = c.id AND child.isDeleted = false
        )))
        ORDER BY c.sortOrder ASC
    """
    )
    fun findAllCommentsByParentIds(
        siteId: Long,
        pageId: String,
        parentIds: List<Long>
    ): List<Comment>

    @Query("SELECT c FROM Comment c WHERE c.siteId = :siteId AND c.pageId = :pageId AND c.sortOrder > :sortOrder ORDER BY c.sortOrder ASC")
    fun findBySiteIdAndPageIdAndSortOrderGreaterThan(
        siteId: Long,
        pageId: String,
        sortOrder: BigDecimal
    ): List<Comment>

    @Query("SELECT c FROM Comment c WHERE c.siteId = :siteId AND c.pageId = :pageId AND c.sortOrder >= :sortOrder ORDER BY c.sortOrder ASC")
    fun findBySiteIdAndPageIdAndSortOrderGreaterThanOrEqual(
        siteId: Long,
        pageId: String,
        sortOrder: BigDecimal
    ): List<Comment>

    @Query("SELECT c FROM Comment c WHERE c.siteKey = :siteKey AND c.pageId = :pageId AND c.isDeleted = false ORDER BY c.sortOrder ASC")
    fun findBySiteKeyAndPageIdAndIsDeletedFalseOrderBySortOrder(
        siteKey: String,
        pageId: String
    ): List<Comment>

    // 관리자용 추가 쿼리
    fun countBySiteId(siteId: Long): Long
    fun countByCreatedAtAfter(createdAt: LocalDateTime): Long
    fun countByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): Long

    // 플랜 검증용 쿼리 - 사용자별 월간 댓글 수 조회
    @Query(
        """
        SELECT COUNT(c) FROM Comment c 
        JOIN Site s ON c.siteId = s.id 
        WHERE s.ownerId = :ownerId 
        AND c.createdAt BETWEEN :startDate AND :endDate
        AND c.isDeleted = false
    """
    )
    fun countByOwnerIdAndCreatedAtBetween(
        ownerId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Long

    // 사용자 관리용 쿼리
    fun findByUserIdAndIsDeletedFalse(userId: Long): List<Comment>
    fun findByUserIdAndSiteIdInAndIsDeletedFalse(userId: Long, siteIds: List<Long>): List<Comment>
    fun findTopByUserIdAndSiteIdAndIsDeletedFalseOrderByCreatedAtDesc(userId: Long, siteId: Long): Comment?
    fun findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Comment>
    fun findByUserIdAndSiteIdInAndIsDeletedFalseOrderByCreatedAtDesc(userId: Long, siteIds: List<Long>, pageable: Pageable): Page<Comment>

    // 관리자용 댓글 조회 메소드들
    @Query("SELECT DISTINCT c.pageId FROM Comment c WHERE c.siteId = :siteId ORDER BY c.pageId")
    fun findDistinctPageIdsBySiteId(siteId: Long): List<String>

    fun findBySiteId(siteId: Long, pageable: Pageable): Page<Comment>
    fun findBySiteIdAndContentContainingIgnoreCase(siteId: Long, content: String, pageable: Pageable): Page<Comment>
    fun findBySiteIdAndPageIdAndContentContainingIgnoreCase(siteId: Long, pageId: String, content: String, pageable: Pageable): Page<Comment>

    // 권한별 통계용 쿼리 추가
    fun countBySiteIdIn(siteIds: List<Long>): Long
    fun countBySiteIdInAndCreatedAtAfter(siteIds: List<Long>, createdAt: LocalDateTime): Long
    fun countBySiteIdInAndCreatedAtBetween(siteIds: List<Long>, start: LocalDateTime, end: LocalDateTime): Long

    // 결제 관리용 추가 쿼리
    fun findBySiteIdInAndUserId(siteIds: List<Long>, userId: Long): List<Comment>

    // 관리자용 댓글 조회 메소드들 (권한별 필터링)
    fun findByIsDeletedFalse(pageable: Pageable): Page<Comment>
    fun findByPageIdAndIsDeletedFalse(pageId: String, pageable: Pageable): Page<Comment>
    fun findBySiteIdInAndIsDeletedFalse(siteIds: List<Long>, pageable: Pageable): Page<Comment>
    fun findBySiteIdInAndPageIdAndIsDeletedFalse(siteIds: List<Long>, pageId: String, pageable: Pageable): Page<Comment>

    // 상태별 댓글 통계 조회
    fun countByStatus(status: CommentStatus): Long
    fun countBySiteIdInAndStatus(siteIds: List<Long>, status: CommentStatus): Long
}
