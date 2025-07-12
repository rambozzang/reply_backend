package com.comdeply.comment.app.web.svc

import com.comdeply.comment.app.web.dto.SubscriptionCancelRequest
import com.comdeply.comment.app.web.dto.SubscriptionResponse
import com.comdeply.comment.app.web.entity.Admin
import com.comdeply.comment.app.web.entity.Subscription
import com.comdeply.comment.app.web.entity.SubscriptionPlan
import com.comdeply.comment.app.web.entity.SubscriptionStatus
import com.comdeply.comment.app.web.repository.AdminRepository
import com.comdeply.comment.app.web.repository.SubscriptionRepository
import com.comdeply.comment.portone.service.PortOneSubscriptionService
import com.comdeply.comment.utils.PlanLimits
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val adminRepository: AdminRepository,
    private val portOneSubscriptionService: PortOneSubscriptionService
) {
    private val logger = LoggerFactory.getLogger(SubscriptionService::class.java)

    // PortOne 통합 구독 생성 메서드 추가
    fun createPortOneSubscription(
        adminId: Long,
        plan: SubscriptionPlan
    ): Subscription {
        return portOneSubscriptionService.startSubscription(adminId, plan)
    }

    /**
     * 구독 생성
     */
    fun createSubscription(
        admin: Admin,
        planId: String,
        planName: String,
        amount: Int
    ): Subscription {
        logger.info("구독 생성 시작: 관리자=${admin.id}, 플랜=$planId")

        // 기존 활성 구독 취소
        val existingSubscription =
            subscriptionRepository.findByAdminIdAndStatus(
                admin.id,
                SubscriptionStatus.ACTIVE
            )
        existingSubscription?.let {
            val cancelledSubscription =
                it.copy(
                    status = SubscriptionStatus.CANCELLED,
                    cancelledAt = LocalDateTime.now(),
                    cancelReason = "새 구독으로 인한 자동 취소",
                    updatedAt = LocalDateTime.now()
                )
            subscriptionRepository.save(cancelledSubscription)
            logger.info("기존 구독 취소: ${it.id}")
        }

        // 새 구독 생성
        val startDate = LocalDateTime.now()
        val endDate = startDate.plusMonths(1)
        val nextBillingDate = endDate

        val subscription =
            Subscription(
                adminId = admin.id,
                plan = SubscriptionPlan.valueOf(planId.uppercase()),
                status = SubscriptionStatus.ACTIVE,
                startDate = startDate,
                endDate = endDate,
                nextBillingDate = nextBillingDate,
                billingCycle = "MONTHLY"
            )

        return subscriptionRepository.save(subscription).also {
            logger.info("구독 생성 완료: 구독ID=${it.id}, 관리자=${admin.id}, 플랜=$planId")
        }
    }

    /**
     * 관리자 구독 정보 조회
     */
    fun getAdminSubscription(adminId: Long): SubscriptionResponse? {
        val subscription =
            subscriptionRepository.findByAdminIdAndStatus(
                adminId,
                SubscriptionStatus.ACTIVE
            ) ?: return null

        val planLimits = PlanLimits.getLimits(subscription.plan)
        
        return SubscriptionResponse(
            id = subscription.id,
            userId = subscription.adminId,
            planId = subscription.plan.name.lowercase(),
            planName = subscription.plan.displayName,
            price = planLimits.monthlyPrice.toInt(),
            monthlyCommentLimit = planLimits.monthlyCommentLimit,
            currentCommentCount = 0, // TODO: 현재 댓글 수 조회 로직 필요
            status = subscription.status.name.lowercase(),
            startDate = subscription.startDate.toString(),
            nextBillingDate = subscription.nextBillingDate?.toString(),
            cancelledAt = null, // TODO: cancelledAt 필드 추가 필요
            cancelReason = null, // TODO: cancelReason 필드 추가 필요
            createdAt = subscription.createdAt.toString(),
            updatedAt = subscription.updatedAt.toString()
        )
    }

    /**
     * 구독 취소
     */
    fun cancelSubscription(
        subscriptionId: Long,
        request: SubscriptionCancelRequest
    ): Boolean {
        logger.info("구독 취소 요청: 구독ID=$subscriptionId, 사유=${request.reason}")

        val subscription =
            subscriptionRepository.findById(subscriptionId).orElse(null)
                ?: throw IllegalArgumentException("구독을 찾을 수 없습니다: $subscriptionId")

        if (subscription.status != SubscriptionStatus.ACTIVE) {
            throw IllegalStateException("취소 가능한 상태가 아닙니다: ${subscription.status}")
        }

        val cancelledSubscription =
            subscription.copy(
                status = SubscriptionStatus.CANCELLED,
                cancelledAt = LocalDateTime.now(),
                cancelReason = request.reason ?: "사용자 요청",
                autoRenewal = false,
                updatedAt = LocalDateTime.now()
            )

        subscriptionRepository.save(cancelledSubscription)

        logger.info("구독 취소 완료: 구독ID=$subscriptionId")
        return true
    }

    /**
     * 관리자 ID로 구독 취소
     */
    fun cancelSubscriptionByAdminId(adminId: Long): Boolean {
        logger.info("관리자 구독 취소: 관리자ID=$adminId")

        val subscription =
            subscriptionRepository.findByAdminIdAndStatus(
                adminId,
                SubscriptionStatus.ACTIVE
            ) ?: return false

        val cancelledSubscription =
            subscription.copy(
                status = SubscriptionStatus.CANCELLED,
                cancelledAt = LocalDateTime.now(),
                cancelReason = "결제 취소로 인한 자동 취소",
                autoRenewal = false,
                updatedAt = LocalDateTime.now()
            )

        subscriptionRepository.save(cancelledSubscription)

        logger.info("관리자 구독 취소 완료: 관리자ID=$adminId, 구독ID=${subscription.id}")
        return true
    }

    /**
     * 댓글 수 증가 (관리자 기준) - 현재는 PlanLimits 기반으로 체크
     */
    fun incrementCommentCount(adminId: Long): Boolean {
        logger.debug("댓글 수 증가: 관리자ID=$adminId")

        val subscription =
            subscriptionRepository.findByAdminIdAndStatus(
                adminId,
                SubscriptionStatus.ACTIVE
            ) ?: return false

        val planLimits = PlanLimits.getLimits(subscription.plan)
        
        // TODO: 실제 댓글 카운터 구현 필요 (현재는 항상 true 리턴)
        logger.debug("댓글 수 증가 완료: 관리자ID=$adminId, 플랜=${subscription.plan}, 한도=${planLimits.monthlyCommentLimit}")
        return true
    }

    /**
     * 댓글 한도 확인 (관리자 기준) - 현재는 PlanLimits 기반으로 체크
     */
    fun checkCommentLimit(adminId: Long): Boolean {
        val subscription =
            subscriptionRepository.findByAdminIdAndStatus(
                adminId,
                SubscriptionStatus.ACTIVE
            ) ?: return false

        val planLimits = PlanLimits.getLimits(subscription.plan)
        
        // TODO: 실제 댓글 카운터 구현 필요 (현재는 항상 true 리턴)
        logger.debug("댓글 한도 확인: 관리자ID=$adminId, 플랜=${subscription.plan}, 한도=${planLimits.monthlyCommentLimit}")
        return true
    }

    /**
     * 구독 갱신 처리 (스케줄러에서 호출)
     */
    fun processSubscriptionRenewals() {
        logger.info("구독 갱신 처리 시작")

        val now = LocalDateTime.now()
        val renewalWindow = now.plusDays(1) // 1일 전 갱신 처리

        val subscriptionsToRenew =
            subscriptionRepository.findSubscriptionsForRenewal(
                SubscriptionStatus.ACTIVE,
                now,
                renewalWindow
            )

        logger.info("갱신 대상 구독: ${subscriptionsToRenew.size}개")

        subscriptionsToRenew.forEach { subscription ->
            try {
                renewSubscription(subscription)
            } catch (e: Exception) {
                logger.error("구독 갱신 실패: 구독ID=${subscription.id}", e)
            }
        }

        logger.info("구독 갱신 처리 완료")
    }

    /**
     * 만료된 구독 처리 (스케줄러에서 호출)
     */
    fun processExpiredSubscriptions() {
        logger.info("만료된 구독 처리 시작")

        val now = LocalDateTime.now()
        val expiredSubscriptions =
            subscriptionRepository.findExpiredSubscriptions(
                SubscriptionStatus.ACTIVE,
                now
            )

        logger.info("만료된 구독: ${expiredSubscriptions.size}개")

        expiredSubscriptions.forEach { subscription ->
            val expiredSubscription =
                subscription.copy(
                    status = SubscriptionStatus.EXPIRED,
                    updatedAt = LocalDateTime.now()
                )
            subscriptionRepository.save(expiredSubscription)

            logger.info("구독 만료 처리: 구독ID=${subscription.id}, 관리자ID=${subscription.admin.id}")
        }

        logger.info("만료된 구독 처리 완료")
    }

    /**
     * 개별 구독 갱신
     */
    private fun renewSubscription(subscription: Subscription) {
        logger.info("구독 갱신: 구독ID=${subscription.id}")

        // 새로운 구독 기간 설정
        val newStartDate = subscription.endDate
        val newEndDate = newStartDate.plusMonths(1)
        val newNextBillingDate = newEndDate

        val renewedSubscription =
            subscription.copy(
                startDate = newStartDate,
                endDate = newEndDate,
                nextBillingDate = newNextBillingDate,
                currentCommentCount = 0, // 댓글 수 초기화
                updatedAt = LocalDateTime.now()
            )

        subscriptionRepository.save(renewedSubscription)

        logger.info("구독 갱신 완료: 구독ID=${subscription.id}, 새 종료일=$newEndDate")
    }

    /**
     * 구독 업그레이드
     */
    fun upgradeSubscription(adminId: Long, newPlanId: String): SubscriptionResponse {
        logger.info("구독 업그레이드 요청: 관리자=$adminId, 새플랜=$newPlanId")

        val currentSubscription = subscriptionRepository.findByAdminIdAndStatus(
            adminId,
            SubscriptionStatus.ACTIVE
        ) ?: throw IllegalArgumentException("활성 구독을 찾을 수 없습니다")

        // 플랜 정보 가져오기
        val planInfo = getPlanInfo(newPlanId)

        // 현재 플랜보다 상위 플랜인지 확인
        if (getPlanLevel(currentSubscription.planId) >= getPlanLevel(newPlanId)) {
            throw IllegalArgumentException("현재 플랜보다 상위 플랜으로만 업그레이드할 수 있습니다")
        }

        // 새 구독 생성 (기존 구독은 createSubscription에서 자동 취소됨)
        val admin = currentSubscription.admin
        val newSubscription = createSubscription(admin, newPlanId, planInfo.name, planInfo.price)

        return SubscriptionResponse(
            id = newSubscription.id,
            userId = newSubscription.admin.id,
            planId = newSubscription.planId,
            planName = newSubscription.planName,
            price = newSubscription.amount,
            monthlyCommentLimit = newSubscription.monthlyCommentLimit,
            currentCommentCount = newSubscription.currentCommentCount,
            status = newSubscription.status.name.lowercase(),
            startDate = newSubscription.startDate.toString(),
            nextBillingDate = newSubscription.nextBillingDate?.toString(),
            cancelledAt = newSubscription.cancelledAt?.toString(),
            cancelReason = newSubscription.cancelReason,
            createdAt = newSubscription.createdAt.toString(),
            updatedAt = newSubscription.updatedAt.toString()
        )
    }

    /**
     * 구독 다운그레이드
     */
    fun downgradeSubscription(adminId: Long, newPlanId: String): SubscriptionResponse {
        logger.info("구독 다운그레이드 요청: 관리자=$adminId, 새플랜=$newPlanId")

        val currentSubscription = subscriptionRepository.findByAdminIdAndStatus(
            adminId,
            SubscriptionStatus.ACTIVE
        ) ?: throw IllegalArgumentException("활성 구독을 찾을 수 없습니다")

        // 플랜 정보 가져오기
        val planInfo = getPlanInfo(newPlanId)

        // 현재 플랜보다 하위 플랜인지 확인
        if (getPlanLevel(currentSubscription.planId) <= getPlanLevel(newPlanId)) {
            throw IllegalArgumentException("현재 플랜보다 하위 플랜으로만 다운그레이드할 수 있습니다")
        }

        // 새 구독 생성 (기존 구독은 createSubscription에서 자동 취소됨)
        val admin = currentSubscription.admin
        val newSubscription = createSubscription(admin, newPlanId, planInfo.name, planInfo.price)

        return SubscriptionResponse(
            id = newSubscription.id,
            userId = newSubscription.admin.id,
            planId = newSubscription.planId,
            planName = newSubscription.planName,
            price = newSubscription.amount,
            monthlyCommentLimit = newSubscription.monthlyCommentLimit,
            currentCommentCount = newSubscription.currentCommentCount,
            status = newSubscription.status.name.lowercase(),
            startDate = newSubscription.startDate.toString(),
            nextBillingDate = newSubscription.nextBillingDate?.toString(),
            cancelledAt = newSubscription.cancelledAt?.toString(),
            cancelReason = newSubscription.cancelReason,
            createdAt = newSubscription.createdAt.toString(),
            updatedAt = newSubscription.updatedAt.toString()
        )
    }

    /**
     * 플랜 정보 가져오기
     */
    private fun getPlanInfo(planId: String): PlanInfo {
        return when (planId) {
            "starter" -> PlanInfo("Starter 플랜", 0)
            "pro" -> PlanInfo("Pro 플랜", 29000)
            "enterprise" -> PlanInfo("Enterprise 플랜", 99000)
            else -> throw IllegalArgumentException("알 수 없는 플랜입니다: $planId")
        }
    }

    /**
     * 플랜 레벨 가져오기 (업그레이드/다운그레이드 판단용)
     */
    private fun getPlanLevel(planId: String): Int {
        return when (planId) {
            "starter" -> 1
            "pro" -> 2
            "enterprise" -> 3
            else -> 0
        }
    }

    /**
     * 플랜 정보 클래스
     */
    private data class PlanInfo(
        val name: String,
        val price: Int
    )

    /**
     * 구독 통계 조회
     */
    fun getSubscriptionStats(): Map<String, Any> {
        val activeCount = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE)
        val cancelledCount = subscriptionRepository.countByStatus(SubscriptionStatus.CANCELLED)
        val expiredCount = subscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED)

        val proCount = subscriptionRepository.countByPlanIdAndStatus("pro", SubscriptionStatus.ACTIVE)
        val businessCount = subscriptionRepository.countByPlanIdAndStatus("business", SubscriptionStatus.ACTIVE)

        return mapOf(
            "active" to activeCount,
            "cancelled" to cancelledCount,
            "expired" to expiredCount,
            "total" to (activeCount + cancelledCount + expiredCount),
            "planStats" to
                mapOf(
                    "pro" to proCount,
                    "business" to businessCount
                )
        )
    }
}
