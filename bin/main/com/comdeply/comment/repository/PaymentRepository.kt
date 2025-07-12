package com.comdeply.comment.repository

import com.comdeply.comment.entity.Payment
import com.comdeply.comment.entity.PaymentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PaymentRepository : JpaRepository<Payment, Long> {
    // 결제 ID로 조회
    fun findByPaymentId(paymentId: String): Payment?

    // 관리자별 결제 내역 조회 (페이징)
    fun findByAdminIdOrderByCreatedAtDesc(
        adminId: Long,
        pageable: Pageable
    ): Page<Payment>

    // 관리자별 특정 상태의 결제 조회
    fun findByAdminIdAndStatus(
        adminId: Long,
        status: PaymentStatus
    ): List<Payment>

    // 관리자의 최근 성공한 결제 조회
    fun findFirstByAdminIdAndStatusOrderByPaidAtDesc(
        adminId: Long,
        status: PaymentStatus
    ): Payment?

    // 특정 기간 내 결제 조회
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    fun findByCreatedAtBetween(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Payment>

    // 관리자별 총 결제 금액 조회
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.admin.id = :adminId AND p.status = :status")
    fun getTotalAmountByAdminIdAndStatus(
        @Param("adminId") adminId: Long,
        @Param("status") status: PaymentStatus
    ): Long

    // 월별 결제 통계
    @Query(
        """
        SELECT DATE_FORMAT(p.paidAt, '%Y-%m') as month, COUNT(p) as count, SUM(p.amount) as total
        FROM Payment p 
        WHERE p.status = :status 
        AND p.paidAt BETWEEN :startDate AND :endDate
        GROUP BY DATE_FORMAT(p.paidAt, '%Y-%m')
        ORDER BY month DESC
    """
    )
    fun getMonthlyPaymentStats(
        @Param("status") status: PaymentStatus,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    // 결제 상태별 개수 조회
    @Query("SELECT p.status, COUNT(p) FROM Payment p GROUP BY p.status")
    fun getPaymentCountByStatus(): List<Array<Any>>

    // 특정 플랜의 결제 개수 조회
    fun countByPlanIdAndStatus(
        planId: String,
        status: PaymentStatus
    ): Long

    // 관리자용 추가 메서드들
    // 상태별 페이징 조회
    fun findByStatus(
        status: PaymentStatus,
        pageable: Pageable
    ): Page<Payment>

    // 여러 관리자 ID로 결제 조회 (페이징)
    fun findByAdminIdIn(
        adminIds: List<Long>,
        pageable: Pageable
    ): Page<Payment>

    // 여러 관리자 ID와 상태로 결제 조회 (페이징)
    fun findByAdminIdInAndStatus(
        adminIds: List<Long>,
        status: PaymentStatus,
        pageable: Pageable
    ): Page<Payment>

    // 전체 수익 조회
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = com.comdeply.comment.entity.PaymentStatus.PAID")
    fun getTotalRevenue(): Long

    // 여러 관리자 ID의 총 수익 조회
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.admin.id IN :adminIds AND p.status = com.comdeply.comment.entity.PaymentStatus.PAID")
    fun getTotalRevenueByAdminIds(@Param("adminIds") adminIds: List<Long>): Long

    // 상태별 개수 조회
    fun countByStatus(status: PaymentStatus): Long

    // 여러 관리자 ID의 결제 개수 조회
    fun countByAdminIdIn(adminIds: List<Long>): Long

    // 여러 관리자 ID와 상태별 개수 조회
    fun countByAdminIdInAndStatus(
        adminIds: List<Long>,
        status: PaymentStatus
    ): Long

    // 월별 통계 (전체)
    @Query(
        value = """
        SELECT DATE_FORMAT(p.paid_at, '%Y-%m') as month, COUNT(p.id) as count, SUM(p.amount) as total
        FROM payments p 
        WHERE p.status = 'PAID'
        AND p.paid_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH)
        GROUP BY DATE_FORMAT(p.paid_at, '%Y-%m')
        ORDER BY month DESC
    """,
        nativeQuery = true
    )
    fun getMonthlyStats(): List<Array<Any>>

    // 월별 통계 (특정 관리자들)
    @Query(
        value = """
        SELECT DATE_FORMAT(p.paid_at, '%Y-%m') as month, COUNT(p.id) as count, SUM(p.amount) as total
        FROM payments p 
        WHERE p.admin_id IN :adminIds 
        AND p.status = 'PAID'
        AND p.paid_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH)
        GROUP BY DATE_FORMAT(p.paid_at, '%Y-%m')
        ORDER BY month DESC
    """,
        nativeQuery = true
    )
    fun getMonthlyStatsByAdminIds(@Param("adminIds") adminIds: List<Long>): List<Array<Any>>
}
