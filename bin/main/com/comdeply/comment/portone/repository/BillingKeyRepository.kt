package com.comdeply.comment.portone.repository

import com.comdeply.comment.portone.entity.BillingKey
import com.comdeply.comment.portone.entity.BillingKeyStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BillingKeyRepository : JpaRepository<BillingKey, Long> {
    fun findByAdminIdAndStatus(adminId: Long, status: BillingKeyStatus): BillingKey?
    fun findByBillingKey(billingKey: String): BillingKey?
    fun findByAdminId(adminId: Long): List<BillingKey>
    fun existsByAdminIdAndStatus(adminId: Long, status: BillingKeyStatus): Boolean
}