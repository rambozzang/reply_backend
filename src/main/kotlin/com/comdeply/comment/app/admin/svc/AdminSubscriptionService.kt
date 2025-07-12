package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.app.admin.svc.vo.SubscriptionListResponse
import com.comdeply.comment.app.admin.svc.vo.SubscriptionStatsResponse
import com.comdeply.comment.app.admin.svc.vo.SubscriptionUpdateRequest
import com.comdeply.comment.dto.SubscriptionResponse
import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.Subscription
import com.comdeply.comment.entity.SubscriptionStatus
import com.comdeply.comment.repository.AdminRepository
import com.comdeply.comment.repository.SubscriptionRepository
import com.comdeply.comment.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class AdminSubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val userRepository: UserRepository,
    private val adminRepository: AdminRepository
) {
    private val logger = LoggerFactory.getLogger(AdminSubscriptionService::class.java)

    fun getSubscriptions(
        admin: Admin,
        page: Int,
        size: Int,
        status: String?,
        planId: String?,
        search: String?
    ): Page<SubscriptionListResponse> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        // 일반 관리자는 자신이 관리하는 사이트의 구독만 조회 가능
        return if (admin.role.name == "SUPER_ADMIN") {
            // 수퍼관리자는 모든 구독 조회
            subscriptionRepository.findAllSubscriptionsWithFilters(status, planId, search, pageable)
                .map { subscription -> convertToSubscriptionListResponse(subscription) }
        } else {
            // 일반 관리자는 자신이 관리하는 사이트의 구독만 조회
            subscriptionRepository.findSubscriptionsByAdminWithFilters(admin.id, status, planId, search, pageable)
                .map { subscription -> convertToSubscriptionListResponse(subscription) }
        }
    }

    fun getSubscriptionDetail(subscriptionId: Long, admin: Admin): SubscriptionResponse {
        val subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow { IllegalArgumentException("구독을 찾을 수 없습니다: $subscriptionId") }

        // 권한 체크
        if (admin.role.name != "SUPER_ADMIN") {
            // 일반 관리자는 자신이 관리하는 사이트의 구독만 조회 가능
            val hasPermission = subscriptionRepository.hasPermissionToViewSubscription(admin.id, subscriptionId)
            if (!hasPermission) {
                throw IllegalArgumentException("해당 구독에 대한 권한이 없습니다")
            }
        }

        return convertToSubscriptionResponse(subscription)
    }

    fun updateSubscription(
        subscriptionId: Long,
        request: SubscriptionUpdateRequest,
        admin: Admin
    ): SubscriptionResponse {
        val subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow { IllegalArgumentException("구독을 찾을 수 없습니다: $subscriptionId") }

        // 권한 체크
        if (admin.role.name != "SUPER_ADMIN") {
            val hasPermission = subscriptionRepository.hasPermissionToViewSubscription(admin.id, subscriptionId)
            if (!hasPermission) {
                throw IllegalArgumentException("해당 구독에 대한 권한이 없습니다")
            }
        }

        // 구독 정보 업데이트
        request.planId?.let { subscription.planId = it }
        request.planName?.let { subscription.planName = it }
        request.monthlyCommentLimit?.let { subscription.monthlyCommentLimit = it }
        request.price?.let { subscription.amount = it }
        request.status?.let {
            subscription.status = when (it) {
                "active" -> SubscriptionStatus.ACTIVE
                "cancelled" -> SubscriptionStatus.CANCELLED
                "expired" -> SubscriptionStatus.EXPIRED
                else -> SubscriptionStatus.INACTIVE
            }
        }
        request.nextBillingDate?.let {
            subscription.nextBillingDate = LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }

        subscription.updatedAt = LocalDateTime.now()
        val updatedSubscription = subscriptionRepository.save(subscription)

        logger.info("구독 수정 완료: subscriptionId={}, adminId={}", subscriptionId, admin.id)
        return convertToSubscriptionResponse(updatedSubscription)
    }

    fun cancelSubscription(subscriptionId: Long, reason: String, admin: Admin) {
        val subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow { IllegalArgumentException("구독을 찾을 수 없습니다: $subscriptionId") }

        // 권한 체크
        if (admin.role.name != "SUPER_ADMIN") {
            val hasPermission = subscriptionRepository.hasPermissionToViewSubscription(admin.id, subscriptionId)
            if (!hasPermission) {
                throw IllegalArgumentException("해당 구독에 대한 권한이 없습니다")
            }
        }

        subscription.status = SubscriptionStatus.CANCELLED
        subscription.cancelledAt = LocalDateTime.now()
        subscription.cancelReason = reason
        subscription.updatedAt = LocalDateTime.now()

        subscriptionRepository.save(subscription)
        logger.info("구독 취소 완료: subscriptionId={}, reason={}, adminId={}", subscriptionId, reason, admin.id)
    }

    fun reactivateSubscription(subscriptionId: Long, admin: Admin) {
        val subscription = subscriptionRepository.findById(subscriptionId)
            .orElseThrow { IllegalArgumentException("구독을 찾을 수 없습니다: $subscriptionId") }

        // 권한 체크
        if (admin.role.name != "SUPER_ADMIN") {
            val hasPermission = subscriptionRepository.hasPermissionToViewSubscription(admin.id, subscriptionId)
            if (!hasPermission) {
                throw IllegalArgumentException("해당 구독에 대한 권한이 없습니다")
            }
        }

        if (subscription.status != SubscriptionStatus.CANCELLED) {
            throw IllegalArgumentException("취소된 구독만 재활성화할 수 있습니다")
        }

        subscription.status = SubscriptionStatus.ACTIVE
        subscription.cancelledAt = null
        subscription.cancelReason = null
        subscription.updatedAt = LocalDateTime.now()

        subscriptionRepository.save(subscription)
        logger.info("구독 재활성화 완료: subscriptionId={}, adminId={}", subscriptionId, admin.id)
    }

    fun getSubscriptionStats(admin: Admin, startDate: String?, endDate: String?): SubscriptionStatsResponse {
        val start = startDate?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        val end = endDate?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }

        return if (admin.role.name == "SUPER_ADMIN") {
            // 수퍼관리자는 전체 통계
            val totalSubscriptions = subscriptionRepository.countTotalSubscriptions(start, end)
            val activeSubscriptions = subscriptionRepository.countActiveSubscriptions(start, end)
            val cancelledSubscriptions = subscriptionRepository.countCancelledSubscriptions(start, end)
            val monthlyRevenue = subscriptionRepository.calculateMonthlyRevenue(start, end)
            val paidSubscriptions = subscriptionRepository.countPaidSubscriptions(start, end)

            SubscriptionStatsResponse(
                totalSubscriptions = totalSubscriptions,
                activeSubscriptions = activeSubscriptions,
                cancelledSubscriptions = cancelledSubscriptions,
                monthlyRevenue = monthlyRevenue,
                paidSubscriptions = paidSubscriptions,
                period = if (startDate != null && endDate != null) "$startDate ~ $endDate" else "전체"
            )
        } else {
            // 일반 관리자는 자신이 관리하는 사이트의 통계
            val totalSubscriptions = subscriptionRepository.countSubscriptionsByAdmin(admin.id, start, end)
            val activeSubscriptions = subscriptionRepository.countActiveSubscriptionsByAdmin(admin.id, start, end)
            val cancelledSubscriptions = subscriptionRepository.countCancelledSubscriptionsByAdmin(admin.id, start, end)
            val monthlyRevenue = subscriptionRepository.calculateMonthlyRevenueByAdmin(admin.id, start, end)
            val paidSubscriptions = subscriptionRepository.countPaidSubscriptionsByAdmin(admin.id, start, end)

            SubscriptionStatsResponse(
                totalSubscriptions = totalSubscriptions,
                activeSubscriptions = activeSubscriptions,
                cancelledSubscriptions = cancelledSubscriptions,
                monthlyRevenue = monthlyRevenue,
                paidSubscriptions = paidSubscriptions,
                period = if (startDate != null && endDate != null) "$startDate ~ $endDate" else "전체"
            )
        }
    }

    fun getUserSubscription(userId: Long, admin: Admin): SubscriptionResponse? {
        val subscription = subscriptionRepository.findByAdminId(userId) // userId는 실제로 adminId
            ?: return null

        // 권한 체크
        if (admin.role.name != "SUPER_ADMIN") {
            val hasPermission = subscriptionRepository.hasPermissionToViewSubscription(admin.id, subscription.id)
            if (!hasPermission) {
                throw IllegalArgumentException("해당 구독에 대한 권한이 없습니다")
            }
        }

        return convertToSubscriptionResponse(subscription)
    }

    private fun convertToSubscriptionListResponse(subscription: Subscription): SubscriptionListResponse {
        val admin = adminRepository.findById(subscription.admin.id).orElse(null)

        return SubscriptionListResponse(
            id = subscription.id,
            userId = subscription.admin.id, // admin ID를 userId로 사용
            user = admin?.let {
                SubscriptionListResponse.UserInfo(
                    id = it.id,
                    name = it.name,
                    email = it.email
                )
            },
            planId = subscription.planId,
            planName = subscription.planName,
            price = subscription.amount,
            monthlyCommentLimit = subscription.monthlyCommentLimit,
            status = subscription.status.name.lowercase(),
            startDate = subscription.startDate.toString(),
            nextBillingDate = subscription.nextBillingDate?.toString(),
            cancelledAt = subscription.cancelledAt?.toString(),
            cancelReason = subscription.cancelReason,
            createdAt = subscription.createdAt.toString(),
            updatedAt = subscription.updatedAt.toString()
        )
    }

    private fun convertToSubscriptionResponse(subscription: Subscription): SubscriptionResponse {
        val admin = adminRepository.findById(subscription.admin.id).orElse(null)

        return SubscriptionResponse(
            id = subscription.id,
            userId = subscription.admin.id, // admin ID를 userId로 사용
            planId = subscription.planId,
            planName = subscription.planName,
            price = subscription.amount,
            monthlyCommentLimit = subscription.monthlyCommentLimit,
            currentCommentCount = subscription.currentCommentCount,
            status = subscription.status.name.lowercase(),
            startDate = subscription.startDate.toString(),
            nextBillingDate = subscription.nextBillingDate?.toString(),
            cancelledAt = subscription.cancelledAt?.toString(),
            cancelReason = subscription.cancelReason,
            createdAt = subscription.createdAt.toString(),
            updatedAt = subscription.updatedAt.toString()
        )
    }
}
