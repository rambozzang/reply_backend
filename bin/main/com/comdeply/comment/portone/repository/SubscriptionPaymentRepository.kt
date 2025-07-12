package com.comdeply.comment.portone.repository

import com.comdeply.comment.portone.entity.SubscriptionPayment
import com.comdeply.comment.portone.entity.PaymentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SubscriptionPaymentRepository : JpaRepository<SubscriptionPayment, Long> {
    fun findByPaymentId(paymentId: String): SubscriptionPayment?
    fun findByAdminId(adminId: Long, pageable: Pageable): Page<SubscriptionPayment>
    fun findByScheduleId(scheduleId: String): List<SubscriptionPayment>
    fun findByAdminIdAndStatus(adminId: Long, status: PaymentStatus): List<SubscriptionPayment>
    
    @Query("SELECT p FROM SubscriptionPayment p WHERE p.adminId = :adminId AND p.paymentDate BETWEEN :startDate AND :endDate")
    fun findByAdminIdAndPaymentDateBetween(
        @Param("adminId") adminId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<SubscriptionPayment>
    
    @Query("SELECT SUM(p.amount) FROM SubscriptionPayment p WHERE p.adminId = :adminId AND p.status = 'PAID' AND p.paidAt BETWEEN :startDate AND :endDate")
    fun sumPaidAmountByAdminIdAndDateRange(
        @Param("adminId") adminId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): java.math.BigDecimal?
}