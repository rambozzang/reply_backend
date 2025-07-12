package com.comdeply.comment.portone.repository

import com.comdeply.comment.portone.entity.SubscriptionSchedule
import com.comdeply.comment.portone.entity.ScheduleStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SubscriptionScheduleRepository : JpaRepository<SubscriptionSchedule, Long> {
    fun findByAdminIdAndStatus(adminId: Long, status: ScheduleStatus): SubscriptionSchedule?
    fun findByScheduleId(scheduleId: String): SubscriptionSchedule?
    fun findByAdminId(adminId: Long): List<SubscriptionSchedule>
    
    @Query("SELECT s FROM SubscriptionSchedule s WHERE s.status = 'SCHEDULED' AND s.nextPaymentDate <= :paymentDate")
    fun findSchedulesToProcess(@Param("paymentDate") paymentDate: LocalDateTime): List<SubscriptionSchedule>
    
    @Query("SELECT s FROM SubscriptionSchedule s WHERE s.adminId = :adminId AND s.status IN ('SCHEDULED', 'ACTIVE')")
    fun findActiveScheduleByAdminId(@Param("adminId") adminId: Long): SubscriptionSchedule?
}