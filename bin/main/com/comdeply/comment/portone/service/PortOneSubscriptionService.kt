package com.comdeply.comment.portone.service

import com.comdeply.comment.app.web.entity.Subscription
import com.comdeply.comment.app.web.entity.SubscriptionPlan
import com.comdeply.comment.app.web.entity.SubscriptionStatus
import com.comdeply.comment.app.web.repository.SubscriptionRepository
import com.comdeply.comment.portone.dto.BillingCycle
import com.comdeply.comment.portone.dto.*
import com.comdeply.comment.portone.entity.*
import com.comdeply.comment.portone.repository.SubscriptionPaymentRepository
import com.comdeply.comment.portone.repository.SubscriptionScheduleRepository
import com.comdeply.comment.utils.PlanLimits
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Transactional
class PortOneSubscriptionService(
    private val portOneApiClient: PortOneApiClient,
    private val portOneBillingService: PortOneBillingService,
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionScheduleRepository: SubscriptionScheduleRepository,
    private val subscriptionPaymentRepository: SubscriptionPaymentRepository
) {
    
    private val logger = LoggerFactory.getLogger(PortOneSubscriptionService::class.java)

    /**
     * 구독 시작
     */
    fun startSubscription(
        adminId: Long,
        plan: SubscriptionPlan,
        billingCycle: BillingCycle = BillingCycle.MONTHLY
    ): Subscription = runBlocking {
        
        // 빌링키 확인
        val billingKey = portOneBillingService.getBillingKey(adminId)
            ?: throw IllegalStateException("빌링키가 등록되지 않았습니다. 먼저 결제 정보를 등록해주세요.")

        // 기존 활성 구독이 있으면 취소
        val existingSubscription = subscriptionRepository.findByAdminIdAndStatus(adminId, SubscriptionStatus.ACTIVE)
        if (existingSubscription != null) {
            cancelSubscription(adminId)
        }

        try {
            // 첫 번째 결제 실행
            val firstPaymentAmount = getPlanAmount(plan)
            val firstPaymentResult = processSubscriptionPayment(
                adminId = adminId,
                amount = firstPaymentAmount,
                billingKey = billingKey,
                plan = plan,
                description = "${plan.name} 구독 시작"
            )

            if (firstPaymentResult.status != PaymentStatus.PAID) {
                throw RuntimeException("첫 번째 결제가 실패했습니다.")
            }

            // 로컬 구독 생성
            val subscription = Subscription(
                adminId = adminId,
                plan = plan,
                status = SubscriptionStatus.ACTIVE,
                startDate = LocalDateTime.now(),
                nextBillingDate = calculateNextBillingDate(billingCycle),
                billingCycle = billingCycle.name
            )

            val savedSubscription = subscriptionRepository.save(subscription)

            // 구독 스케줄 생성
            createSubscriptionSchedule(
                adminId = adminId,
                subscription = savedSubscription,
                billingKey = billingKey,
                billingCycle = billingCycle
            )

            logger.info("구독 시작 완료: adminId=$adminId, plan=${plan.name}")
            return@runBlocking savedSubscription

        } catch (e: Exception) {
            logger.error("구독 시작 실패: adminId=$adminId, plan=${plan.name}", e)
            throw RuntimeException("구독 시작에 실패했습니다: ${e.message}", e)
        }
    }

    /**
     * 구독 취소
     */
    fun cancelSubscription(adminId: Long): Boolean {
        try {
            // 활성 구독 조회
            val subscription = subscriptionRepository.findByAdminIdAndStatus(adminId, SubscriptionStatus.ACTIVE)
                ?: return false

            // 구독 스케줄 취소
            val schedule = subscriptionScheduleRepository.findByAdminIdAndStatus(adminId, ScheduleStatus.SCHEDULED)
            if (schedule != null) {
                val canceledSchedule = schedule.copy(
                    status = ScheduleStatus.CANCELED,
                    updatedAt = LocalDateTime.now()
                )
                subscriptionScheduleRepository.save(canceledSchedule)
            }

            // 구독 상태 변경
            val canceledSubscription = subscription.copy(
                status = SubscriptionStatus.CANCELED,
                endDate = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            subscriptionRepository.save(canceledSubscription)

            logger.info("구독 취소 완료: adminId=$adminId")
            return true

        } catch (e: Exception) {
            logger.error("구독 취소 실패: adminId=$adminId", e)
            return false
        }
    }

    /**
     * 구독 플랜 변경
     */
    fun changePlan(adminId: Long, newPlan: SubscriptionPlan): Subscription = runBlocking {
        val subscription = subscriptionRepository.findByAdminIdAndStatus(adminId, SubscriptionStatus.ACTIVE)
            ?: throw IllegalStateException("활성 구독이 없습니다.")

        // 플랜 업그레이드/다운그레이드 처리
        val billingKey = portOneBillingService.getBillingKey(adminId)
            ?: throw IllegalStateException("빌링키가 없습니다.")

        val newAmount = getPlanAmount(newPlan)
        val currentAmount = getPlanAmount(subscription.plan)

        // 즉시 차액 결제 (업그레이드의 경우)
        if (newAmount > currentAmount) {
            val prorationAmount = calculateProration(subscription, newAmount, currentAmount)
            if (prorationAmount > BigDecimal.ZERO) {
                processSubscriptionPayment(
                    adminId = adminId,
                    amount = prorationAmount,
                    billingKey = billingKey,
                    plan = newPlan,
                    description = "${newPlan.name}으로 플랜 변경 (차액 결제)"
                )
            }
        }

        // 구독 정보 업데이트
        val updatedSubscription = subscription.copy(
            plan = newPlan,
            updatedAt = LocalDateTime.now()
        )

        val savedSubscription = subscriptionRepository.save(updatedSubscription)

        // 스케줄 업데이트
        updateSubscriptionSchedule(adminId, newPlan)

        logger.info("구독 플랜 변경 완료: adminId=$adminId, ${subscription.plan.name} -> ${newPlan.name}")
        return@runBlocking savedSubscription
    }

    /**
     * 구독 결제 처리
     */
    suspend fun processSubscriptionPayment(
        adminId: Long,
        amount: BigDecimal,
        billingKey: BillingKey,
        plan: SubscriptionPlan,
        description: String,
        scheduleId: String? = null
    ): SubscriptionPayment {
        
        val merchantUid = generateMerchantUid()
        
        val paymentRequest = SubscriptionPaymentRequest(
            customer_uid = billingKey.customerId,
            merchant_uid = merchantUid,
            amount = amount,
            name = description
        )

        try {
            val response = portOneApiClient.requestSubscriptionPayment(paymentRequest)
            
            val paymentStatus = when (response.response?.status) {
                "paid" -> PaymentStatus.PAID
                "failed" -> PaymentStatus.FAILED
                "cancelled" -> PaymentStatus.CANCELED
                else -> PaymentStatus.PENDING
            }

            val subscriptionPayment = SubscriptionPayment(
                adminId = adminId,
                paymentId = merchantUid,
                impUid = response.response?.imp_uid,
                scheduleId = scheduleId,
                amount = amount,
                status = paymentStatus,
                paymentDate = LocalDateTime.now(),
                paidAt = if (paymentStatus == PaymentStatus.PAID) LocalDateTime.now() else null,
                description = description,
                planType = plan.name
            )

            val savedPayment = subscriptionPaymentRepository.save(subscriptionPayment)
            
            if (paymentStatus == PaymentStatus.PAID) {
                logger.info("구독 결제 성공: adminId=$adminId, amount=$amount, merchantUid=$merchantUid")
            } else {
                logger.warn("구독 결제 실패: adminId=$adminId, status=$paymentStatus, merchantUid=$merchantUid")
            }

            return savedPayment

        } catch (e: Exception) {
            logger.error("구독 결제 처리 실패: adminId=$adminId, merchantUid=$merchantUid", e)
            
            // 실패한 결제 정보도 저장
            val failedPayment = SubscriptionPayment(
                adminId = adminId,
                paymentId = merchantUid,
                scheduleId = scheduleId,
                amount = amount,
                status = PaymentStatus.FAILED,
                paymentDate = LocalDateTime.now(),
                description = description,
                planType = plan.name,
                failureReason = e.message
            )
            
            subscriptionPaymentRepository.save(failedPayment)
            throw e
        }
    }

    /**
     * 구독 스케줄 생성
     */
    private fun createSubscriptionSchedule(
        adminId: Long,
        subscription: Subscription,
        billingKey: BillingKey,
        billingCycle: BillingCycle
    ) {
        val scheduleId = generateScheduleId()
        
        val schedule = SubscriptionSchedule(
            adminId = adminId,
            scheduleId = scheduleId,
            subscriptionId = subscription.id!!,
            billingKeyId = billingKey.id!!,
            planType = subscription.plan.name,
            amount = getPlanAmount(subscription.plan),
            billingCycle = billingCycle,
            nextPaymentDate = subscription.nextBillingDate,
            status = ScheduleStatus.SCHEDULED
        )

        subscriptionScheduleRepository.save(schedule)
        logger.info("구독 스케줄 생성: adminId=$adminId, scheduleId=$scheduleId")
    }

    /**
     * 구독 스케줄 업데이트
     */
    private fun updateSubscriptionSchedule(adminId: Long, newPlan: SubscriptionPlan) {
        val schedule = subscriptionScheduleRepository.findByAdminIdAndStatus(adminId, ScheduleStatus.SCHEDULED)
        if (schedule != null) {
            val updatedSchedule = schedule.copy(
                planType = newPlan.name,
                amount = getPlanAmount(newPlan),
                updatedAt = LocalDateTime.now()
            )
            subscriptionScheduleRepository.save(updatedSchedule)
        }
    }

    /**
     * 예약된 결제 처리 (배치 작업용)
     */
    fun processScheduledPayments() = runBlocking {
        val now = LocalDateTime.now()
        val schedulesToProcess = subscriptionScheduleRepository.findSchedulesToProcess(now)

        logger.info("처리할 예약 결제: ${schedulesToProcess.size}건")

        for (schedule in schedulesToProcess) {
            try {
                val billingKey = portOneBillingService.getBillingKey(schedule.adminId)
                if (billingKey == null) {
                    logger.error("빌링키를 찾을 수 없음: adminId=${schedule.adminId}")
                    continue
                }

                val plan = SubscriptionPlan.valueOf(schedule.planType)
                
                processSubscriptionPayment(
                    adminId = schedule.adminId,
                    amount = schedule.amount,
                    billingKey = billingKey,
                    plan = plan,
                    description = "${plan.name} 정기 결제",
                    scheduleId = schedule.scheduleId
                )

                // 다음 결제일 설정
                val nextPaymentDate = calculateNextBillingDate(schedule.billingCycle, schedule.nextPaymentDate)
                val updatedSchedule = schedule.copy(
                    nextPaymentDate = nextPaymentDate,
                    lastPaymentDate = now,
                    updatedAt = now
                )
                subscriptionScheduleRepository.save(updatedSchedule)

            } catch (e: Exception) {
                logger.error("예약 결제 처리 실패: scheduleId=${schedule.scheduleId}", e)
            }
        }
    }

    /**
     * 플랜별 가격 조회
     */
    private fun getPlanAmount(plan: SubscriptionPlan): BigDecimal {
        return when (plan) {
            SubscriptionPlan.FREE -> BigDecimal.ZERO
            SubscriptionPlan.STARTER -> BigDecimal("9900")
            SubscriptionPlan.PRO -> BigDecimal("29900")
            SubscriptionPlan.PREMIUM -> BigDecimal("59900")
            SubscriptionPlan.ENTERPRISE -> BigDecimal("199900")
        }
    }

    /**
     * 다음 결제일 계산
     */
    private fun calculateNextBillingDate(
        billingCycle: BillingCycle,
        baseDate: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime {
        return when (billingCycle) {
            BillingCycle.MONTHLY -> baseDate.plusMonths(1)
            BillingCycle.YEARLY -> baseDate.plusYears(1)
        }
    }

    /**
     * 비례 배분 금액 계산
     */
    private fun calculateProration(
        subscription: Subscription,
        newAmount: BigDecimal,
        currentAmount: BigDecimal
    ): BigDecimal {
        val now = LocalDateTime.now()
        val nextBilling = subscription.nextBillingDate
        val totalDays = ChronoUnit.DAYS.between(subscription.startDate, nextBilling)
        val remainingDays = ChronoUnit.DAYS.between(now, nextBilling)
        
        if (totalDays <= 0 || remainingDays <= 0) return BigDecimal.ZERO
        
        val proration = remainingDays.toDouble() / totalDays.toDouble()
        val difference = newAmount.subtract(currentAmount)
        
        return difference.multiply(BigDecimal.valueOf(proration))
    }

    /**
     * 고유 ID 생성 함수들
     */
    private fun generateMerchantUid(): String {
        return "subscription_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    }

    private fun generateScheduleId(): String {
        return "schedule_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    }

    /**
     * 구독 정보 조회
     */
    fun getSubscription(adminId: Long): Subscription? {
        return subscriptionRepository.findByAdminIdAndStatus(adminId, SubscriptionStatus.ACTIVE)
    }

    /**
     * 결제 내역 조회
     */
    fun getPaymentHistory(adminId: Long): List<SubscriptionPayment> {
        return subscriptionPaymentRepository.findByAdminId(adminId, org.springframework.data.domain.Pageable.unpaged()).content
    }
}