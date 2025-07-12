package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.app.admin.svc.vo.PaymentDetailResponse
import com.comdeply.comment.app.admin.svc.vo.PaymentStatsResponse
import com.comdeply.comment.dto.PaymentResponse
import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.PaymentStatus
import com.comdeply.comment.repository.CommentRepository
import com.comdeply.comment.repository.PaymentRepository
import com.comdeply.comment.repository.SiteRepository
import com.comdeply.comment.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class AdminPaymentService(
    private val paymentRepository: PaymentRepository,
    private val userRepository: UserRepository,
    private val siteRepository: SiteRepository,
    private val commentRepository: CommentRepository,
    private val adminPermissionService: AdminPermissionService
) {
    private val logger = LoggerFactory.getLogger(AdminPaymentService::class.java)

    /**
     * 관리자용 결제 내역 조회
     */
    @Transactional(readOnly = true)
    fun getPaymentsForAdmin(
        admin: Admin,
        page: Int,
        size: Int,
        status: PaymentStatus?,
        startDate: String?,
        endDate: String?
    ): Page<PaymentResponse> {
        logger.info(
            "관리자용 결제 내역 조회: adminId={}, page={}, size={}, status={}",
            admin.id,
            page,
            size,
            status
        )

        val pageable = PageRequest.of(page, size)
        val accessibleSiteIds = adminPermissionService.getAccessibleSiteIds(admin)
        val isGlobalAccess = adminPermissionService.canViewGlobalStats(admin)

        val paymentsPage = if (isGlobalAccess) {
            // 수퍼관리자: 전체 결제 내역 조회
            if (status != null) {
                paymentRepository.findByStatus(status, pageable)
            } else {
                paymentRepository.findAll(pageable)
            }
        } else {
            // 일반 관리자: 담당 사이트의 결제 내역만 조회
            if (accessibleSiteIds.isEmpty()) {
                return Page.empty(pageable)
            }

            val siteAdminIds = siteRepository.findAllById(accessibleSiteIds).map { it.ownerId }
            if (siteAdminIds.isEmpty()) {
                return Page.empty(pageable)
            }

            if (status != null) {
                paymentRepository.findByAdminIdInAndStatus(siteAdminIds, status, pageable)
            } else {
                paymentRepository.findByAdminIdIn(siteAdminIds, pageable)
            }
        }

        logger.info("결제 내역 조회 완료: 총 {}개", paymentsPage.totalElements)
        return paymentsPage.map { payment ->
            PaymentResponse(
                id = payment.id,
                userId = payment.admin.id, // admin ID를 userId로 사용
                amount = payment.amount.toLong(),
                currency = "KRW",
                status = payment.status,
                paymentMethod = payment.paymentMethod,
                transactionId = payment.portoneTransactionId,
                createdAt = payment.createdAt,
                updatedAt = payment.updatedAt,
                description = payment.planName
            )
        }
    }

    /**
     * 관리자용 결제 통계 조회
     */
    @Transactional(readOnly = true)
    fun getPaymentStats(admin: Admin): PaymentStatsResponse {
        logger.info("관리자용 결제 통계 조회: adminId={}", admin.id)

        val accessibleSiteIds = adminPermissionService.getAccessibleSiteIds(admin)
        val isGlobalAccess = adminPermissionService.canViewGlobalStats(admin)

        val stats = if (isGlobalAccess) {
            // 수퍼관리자: 전체 결제 통계
            getGlobalPaymentStats()
        } else {
            // 일반 관리자: 담당 사이트의 결제 통계
            getSitePaymentStats(accessibleSiteIds)
        }

        logger.info("결제 통계 조회 완료: 총 결제 {}건, 총 수익 {}", stats.totalPayments, stats.totalRevenue)
        return stats
    }

    /**
     * 결제 상세 정보 조회
     */
    @Transactional(readOnly = true)
    fun getPaymentDetail(paymentId: Long, admin: Admin): PaymentDetailResponse {
        logger.info("결제 상세 조회: paymentId={}, adminId={}", paymentId, admin.id)

        val payment = paymentRepository.findById(paymentId).orElse(null)
            ?: throw IllegalArgumentException("결제 정보를 찾을 수 없습니다")

        // 권한 확인
        val accessibleSiteIds = adminPermissionService.getAccessibleSiteIds(admin)
        val isGlobalAccess = adminPermissionService.canViewGlobalStats(admin)

        if (!isGlobalAccess) {
            // 관리자가 소유한 사이트들을 조회하여 권한 확인
            val adminSites = siteRepository.findByOwnerId(payment.admin.id)
            val adminSiteIds = adminSites.map { it.id }

            val hasAccessToAdminSites = adminSiteIds.any { siteId -> accessibleSiteIds.contains(siteId) }
            if (!hasAccessToAdminSites && adminSiteIds.isNotEmpty()) {
                throw IllegalArgumentException("해당 결제에 대한 접근 권한이 없습니다")
            }
        }

        val response = PaymentDetailResponse(
            id = payment.id,
            userId = payment.admin.id, // admin ID를 userId로 사용
            paymentId = payment.paymentId,
            planId = payment.planId,
            planName = payment.planName,
            amount = payment.amount,
            status = payment.status,
            paymentMethod = payment.paymentMethod,
            portoneTransactionId = payment.portoneTransactionId,
            cardCompany = payment.cardCompany,
            cardNumber = payment.cardNumber,
            failureReason = payment.failureReason,
            cancelReason = payment.cancelReason,
            createdAt = payment.createdAt,
            updatedAt = payment.updatedAt,
            paidAt = payment.paidAt,
            cancelledAt = payment.cancelledAt,
            refundedAt = payment.refundedAt
        )

        logger.info("결제 상세 조회 완료: paymentId={}, status={}", paymentId, payment.status)
        return response
    }

    /**
     * 전체 결제 통계 조회 (수퍼관리자용)
     */
    private fun getGlobalPaymentStats(): PaymentStatsResponse {
        val totalPayments = paymentRepository.count()
        val totalRevenue = paymentRepository.getTotalRevenue()
        val paidPayments = paymentRepository.countByStatus(PaymentStatus.PAID)
        val pendingPayments = paymentRepository.countByStatus(PaymentStatus.PENDING)
        val failedPayments = paymentRepository.countByStatus(PaymentStatus.FAILED)

        // 오늘 결제 통계 (간단한 방법으로 계산)
        val today = LocalDateTime.now().toLocalDate().atStartOfDay()
        val tomorrow = today.plusDays(1)
        val todayPaymentsList = paymentRepository.findByCreatedAtBetween(today, tomorrow)
        val todayPayments = todayPaymentsList.size.toLong()
        val todayRevenue = todayPaymentsList.filter { it.status == PaymentStatus.PAID }.sumOf { it.amount }.toLong()

        return PaymentStatsResponse(
            totalPayments = totalPayments,
            totalRevenue = totalRevenue,
            paidPayments = paidPayments,
            pendingPayments = pendingPayments,
            failedPayments = failedPayments,
            todayPayments = todayPayments,
            todayRevenue = todayRevenue
        )
    }

    /**
     * 사이트별 결제 통계 조회 (일반 관리자용)
     */
    private fun getSitePaymentStats(accessibleSiteIds: List<Long>): PaymentStatsResponse {
        if (accessibleSiteIds.isEmpty()) {
            return PaymentStatsResponse(
                totalPayments = 0L,
                totalRevenue = 0L,
                paidPayments = 0L,
                pendingPayments = 0L,
                failedPayments = 0L,
                todayPayments = 0L,
                todayRevenue = 0L
            )
        }

        val siteAdminIds = siteRepository.findAllById(accessibleSiteIds).map { it.ownerId }

        if (siteAdminIds.isEmpty()) {
            return PaymentStatsResponse(
                totalPayments = 0L,
                totalRevenue = 0L,
                paidPayments = 0L,
                pendingPayments = 0L,
                failedPayments = 0L,
                todayPayments = 0L,
                todayRevenue = 0L
            )
        }

        val totalPayments = paymentRepository.countByAdminIdIn(siteAdminIds)
        val totalRevenue = paymentRepository.getTotalRevenueByAdminIds(siteAdminIds)
        val paidPayments = paymentRepository.countByAdminIdInAndStatus(siteAdminIds, PaymentStatus.PAID)
        val pendingPayments = paymentRepository.countByAdminIdInAndStatus(siteAdminIds, PaymentStatus.PENDING)
        val failedPayments = paymentRepository.countByAdminIdInAndStatus(siteAdminIds, PaymentStatus.FAILED)

        // 오늘 결제 통계 (간단한 방법으로 계산)
        val today = LocalDateTime.now().toLocalDate().atStartOfDay()
        val tomorrow = today.plusDays(1)
        val todayPaymentsList = paymentRepository.findByCreatedAtBetween(today, tomorrow)
            .filter { payment -> siteAdminIds.contains(payment.admin.id) }
        val todayPayments = todayPaymentsList.size.toLong()
        val todayRevenue = todayPaymentsList.filter { it.status == PaymentStatus.PAID }.sumOf { it.amount }.toLong()

        return PaymentStatsResponse(
            totalPayments = totalPayments,
            totalRevenue = totalRevenue,
            paidPayments = paidPayments,
            pendingPayments = pendingPayments,
            failedPayments = failedPayments,
            todayPayments = todayPayments,
            todayRevenue = todayRevenue
        )
    }
}
