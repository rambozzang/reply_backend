package com.comdeply.comment.repository

import com.comdeply.comment.entity.Subscription
import com.comdeply.comment.entity.SubscriptionStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    // 관리자의 특정 상태 구독 조회
    fun findByAdminIdAndStatus(
        adminId: Long,
        status: SubscriptionStatus
    ): Subscription?

    // 관리자의 모든 구독 조회
    fun findByAdminIdOrderByCreatedAtDesc(adminId: Long): List<Subscription>

    // 관리자의 활성 구독 조회
    fun findByAdminIdAndStatusOrderByCreatedAtDesc(
        adminId: Long,
        status: SubscriptionStatus
    ): List<Subscription>

    // 만료 예정 구독 조회 (자동 갱신용)
    @Query(
        """
        SELECT s FROM Subscription s 
        WHERE s.status = :status 
        AND s.autoRenewal = true 
        AND s.nextBillingDate BETWEEN :startDate AND :endDate
    """
    )
    fun findSubscriptionsForRenewal(
        @Param("status") status: SubscriptionStatus,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Subscription>

    // 만료된 구독 조회
    @Query(
        """
        SELECT s FROM Subscription s 
        WHERE s.status = :status 
        AND s.endDate < :currentDate
    """
    )
    fun findExpiredSubscriptions(
        @Param("status") status: SubscriptionStatus,
        @Param("currentDate") currentDate: LocalDateTime
    ): List<Subscription>

    // 플랜별 활성 구독 수 조회
    fun countByPlanIdAndStatus(
        planId: String,
        status: SubscriptionStatus
    ): Long

    // 전체 활성 구독 수 조회
    fun countByStatus(status: SubscriptionStatus): Long

    // 월별 구독 통계
    @Query(
        """
        SELECT DATE_FORMAT(s.createdAt, '%Y-%m') as month, COUNT(s) as count
        FROM Subscription s 
        WHERE s.status = :status 
        AND s.createdAt BETWEEN :startDate AND :endDate
        GROUP BY DATE_FORMAT(s.createdAt, '%Y-%m')
        ORDER BY DATE_FORMAT(s.createdAt, '%Y-%m') DESC
    """
    )
    fun getMonthlySubscriptionStats(
        @Param("status") status: SubscriptionStatus,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    // 관리자별 댓글 수 업데이트를 위한 조회
    @Query(
        """
        SELECT s FROM Subscription s 
        WHERE s.admin.id = :adminId 
        AND s.status = :status 
        AND s.startDate <= :currentDate 
        AND s.endDate >= :currentDate
    """
    )
    fun findActiveSubscriptionByAdminId(
        @Param("adminId") adminId: Long,
        @Param("status") status: SubscriptionStatus,
        @Param("currentDate") currentDate: LocalDateTime
    ): Subscription?

    // AdminSubscriptionService에서 사용하는 메서드들 추가

    // 관리자별 구독 조회
    fun findByAdminId(adminId: Long): Subscription?

    // 전체 구독 조회 (필터링 포함)
    @Query(
        """
        SELECT s FROM Subscription s 
        WHERE (:status IS NULL OR CAST(s.status AS string) = :status)
        AND (:planId IS NULL OR s.planId = :planId)
        AND (:search IS NULL OR s.admin.name LIKE %:search% OR s.admin.email LIKE %:search%)
        ORDER BY s.createdAt DESC
    """
    )
    fun findAllSubscriptionsWithFilters(
        @Param("status") status: String?,
        @Param("planId") planId: String?,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<Subscription>

    // 관리자별 구독 조회 (필터링 포함)
    @Query(
        """
        SELECT s FROM Subscription s 
        WHERE s.admin.id = :adminId
        AND (:status IS NULL OR CAST(s.status AS string) = :status)
        AND (:planId IS NULL OR s.planId = :planId)
        AND (:search IS NULL OR s.admin.name LIKE %:search% OR s.admin.email LIKE %:search%)
        ORDER BY s.createdAt DESC
    """
    )
    fun findSubscriptionsByAdminWithFilters(
        @Param("adminId") adminId: Long,
        @Param("status") status: String?,
        @Param("planId") planId: String?,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<Subscription>

    // 권한 체크
    @Query(
        """
        SELECT COUNT(s) > 0 FROM Subscription s 
        WHERE s.admin.id = :adminId AND s.id = :subscriptionId
    """
    )
    fun hasPermissionToViewSubscription(
        @Param("adminId") adminId: Long,
        @Param("subscriptionId") subscriptionId: Long
    ): Boolean

    // 통계 조회 메서드들
    @Query(
        """
        SELECT COUNT(s) FROM Subscription s 
        WHERE (:startDate IS NULL OR s.createdAt >= :startDate)
        AND (:endDate IS NULL OR s.createdAt <= :endDate)
    """
    )
    fun countTotalSubscriptions(
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): Long

    @Query(
        """
        SELECT COUNT(s) FROM Subscription s 
        WHERE s.status = com.comdeply.comment.entity.SubscriptionStatus.ACTIVE
        AND (:startDate IS NULL OR s.createdAt >= :startDate)
        AND (:endDate IS NULL OR s.createdAt <= :endDate)
    """
    )
    fun countActiveSubscriptions(
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): Long

    @Query(
        """
        SELECT COUNT(s) FROM Subscription s 
        WHERE s.status = com.comdeply.comment.entity.SubscriptionStatus.CANCELLED
        AND (:startDate IS NULL OR s.createdAt >= :startDate)
        AND (:endDate IS NULL OR s.createdAt <= :endDate)
    """
    )
    fun countCancelledSubscriptions(
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): Long

    @Query(
        """
        SELECT COALESCE(SUM(s.amount), 0) FROM Subscription s 
        WHERE s.status = com.comdeply.comment.entity.SubscriptionStatus.ACTIVE
        AND (:startDate IS NULL OR s.createdAt >= :startDate)
        AND (:endDate IS NULL OR s.createdAt <= :endDate)
    """
    )
    fun calculateMonthlyRevenue(
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): Long

    @Query(
        """
        SELECT COUNT(s) FROM Subscription s 
        WHERE s.status = com.comdeply.comment.entity.SubscriptionStatus.ACTIVE AND s.amount > 0
        AND (:startDate IS NULL OR s.createdAt >= :startDate)
        AND (:endDate IS NULL OR s.createdAt <= :endDate)
    """
    )
    fun countPaidSubscriptions(
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): Long

    // 관리자별 통계 조회 메서드들
    @Query(
        """
        SELECT COUNT(s) FROM Subscription s 
        WHERE s.admin.id = :adminId
        AND (:startDate IS NULL OR s.createdAt >= :startDate)
        AND (:endDate IS NULL OR s.createdAt <= :endDate)
    """
    )
    fun countSubscriptionsByAdmin(
        @Param("adminId") adminId: Long,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): Long

    @Query(
        """
        SELECT COUNT(s) FROM Subscription s 
        WHERE s.admin.id = :adminId AND s.status = com.comdeply.comment.entity.SubscriptionStatus.ACTIVE
        AND (:startDate IS NULL OR s.createdAt >= :startDate)
        AND (:endDate IS NULL OR s.createdAt <= :endDate)
    """
    )
    fun countActiveSubscriptionsByAdmin(
        @Param("adminId") adminId: Long,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): Long

    @Query(
        """
        SELECT COUNT(s) FROM Subscription s 
        WHERE s.admin.id = :adminId AND s.status = com.comdeply.comment.entity.SubscriptionStatus.CANCELLED
        AND (:startDate IS NULL OR s.createdAt >= :startDate)
        AND (:endDate IS NULL OR s.createdAt <= :endDate)
    """
    )
    fun countCancelledSubscriptionsByAdmin(
        @Param("adminId") adminId: Long,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): Long

    @Query(
        """
        SELECT COALESCE(SUM(s.amount), 0) FROM Subscription s 
        WHERE s.admin.id = :adminId AND s.status = com.comdeply.comment.entity.SubscriptionStatus.ACTIVE
        AND (:startDate IS NULL OR s.createdAt >= :startDate)
        AND (:endDate IS NULL OR s.createdAt <= :endDate)
    """
    )
    fun calculateMonthlyRevenueByAdmin(
        @Param("adminId") adminId: Long,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): Long

    @Query(
        """
        SELECT COUNT(s) FROM Subscription s 
        WHERE s.admin.id = :adminId AND s.status = com.comdeply.comment.entity.SubscriptionStatus.ACTIVE AND s.amount > 0
        AND (:startDate IS NULL OR s.createdAt >= :startDate)
        AND (:endDate IS NULL OR s.createdAt <= :endDate)
    """
    )
    fun countPaidSubscriptionsByAdmin(
        @Param("adminId") adminId: Long,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?
    ): Long
}
