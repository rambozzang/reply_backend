package com.comdeply.comment.portone.service

import com.comdeply.comment.app.web.entity.SubscriptionStatus
import com.comdeply.comment.app.web.repository.SubscriptionRepository
import com.comdeply.comment.portone.entity.PaymentStatus
import com.comdeply.comment.portone.entity.ScheduleStatus
import com.comdeply.comment.portone.entity.SubscriptionPayment
import com.comdeply.comment.portone.repository.SubscriptionPaymentRepository
import com.comdeply.comment.portone.repository.SubscriptionScheduleRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class PortOneWebhookService(
    private val portOneApiClient: PortOneApiClient,
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionPaymentRepository: SubscriptionPaymentRepository,
    private val subscriptionScheduleRepository: SubscriptionScheduleRepository,
    private val objectMapper: ObjectMapper
) {
    
    private val logger = LoggerFactory.getLogger(PortOneWebhookService::class.java)

    /**
     * 웹훅 처리
     */
    fun processWebhook(payload: String, signature: String?) = runBlocking {
        logger.info("PortOne 웹훅 수신: payload length=${payload.length}")

        try {
            // 서명 검증 (선택사항)
            if (signature != null && !portOneApiClient.verifyWebhookSignature(payload, signature)) {
                logger.warn("웹훅 서명 검증 실패")
                throw SecurityException("웹훅 서명이 유효하지 않습니다.")
            }

            // JSON 파싱
            val webhookData = objectMapper.readTree(payload)
            val impUid = webhookData.get("imp_uid")?.asText()
            val merchantUid = webhookData.get("merchant_uid")?.asText()
            val status = webhookData.get("status")?.asText()

            if (impUid.isNullOrBlank() || merchantUid.isNullOrBlank()) {
                logger.warn("웹훅 데이터 불완전: imp_uid=$impUid, merchant_uid=$merchantUid")
                return@runBlocking
            }

            logger.info("웹훅 처리 시작: imp_uid=$impUid, merchant_uid=$merchantUid, status=$status")

            // PortOne에서 실제 결제 정보 조회
            val paymentInfo = portOneApiClient.getPaymentInfo(impUid)
            
            if (paymentInfo.code != 0 || paymentInfo.response == null) {
                logger.error("결제 정보 조회 실패: imp_uid=$impUid, code=${paymentInfo.code}")
                return@runBlocking
            }

            val payment = paymentInfo.response

            // 로컬 결제 정보 업데이트
            updatePaymentStatus(merchantUid, paymentInfo.response)

            // 구독 관련 처리
            if (merchantUid.startsWith("subscription_")) {
                handleSubscriptionPayment(merchantUid, paymentInfo.response)
            }

            logger.info("웹훅 처리 완료: imp_uid=$impUid, merchant_uid=$merchantUid")

        } catch (e: Exception) {
            logger.error("웹훅 처리 실패: ${e.message}", e)
            throw e
        }
    }

    /**
     * 결제 상태 업데이트
     */
    private fun updatePaymentStatus(merchantUid: String, paymentData: JsonNode) {
        val subscriptionPayment = subscriptionPaymentRepository.findByPaymentId(merchantUid)
        if (subscriptionPayment == null) {
            logger.warn("결제 정보를 찾을 수 없음: merchant_uid=$merchantUid")
            return
        }

        val status = when (paymentData.get("status")?.asText()) {
            "paid" -> PaymentStatus.PAID
            "failed" -> PaymentStatus.FAILED
            "cancelled" -> PaymentStatus.CANCELED
            "partial_cancelled" -> PaymentStatus.PARTIAL_CANCELED
            else -> PaymentStatus.PENDING
        }

        val paidAt = if (status == PaymentStatus.PAID) {
            paymentData.get("paid_at")?.asLong()?.let { 
                LocalDateTime.ofEpochSecond(it, 0, java.time.ZoneOffset.ofHours(9)) 
            } ?: LocalDateTime.now()
        } else null

        val failureReason = if (status == PaymentStatus.FAILED) {
            paymentData.get("fail_reason")?.asText()
        } else null

        val updatedPayment = subscriptionPayment.copy(
            impUid = paymentData.get("imp_uid")?.asText(),
            status = status,
            paidAt = paidAt,
            failureReason = failureReason,
            updatedAt = LocalDateTime.now()
        )

        subscriptionPaymentRepository.save(updatedPayment)
        logger.info("결제 상태 업데이트: merchant_uid=$merchantUid, status=$status")
    }

    /**
     * 구독 결제 처리
     */
    private fun handleSubscriptionPayment(merchantUid: String, paymentData: JsonNode) {
        val subscriptionPayment = subscriptionPaymentRepository.findByPaymentId(merchantUid) ?: return
        val paymentStatus = subscriptionPayment.status

        when (paymentStatus) {
            PaymentStatus.PAID -> handleSuccessfulPayment(subscriptionPayment)
            PaymentStatus.FAILED -> handleFailedPayment(subscriptionPayment)
            PaymentStatus.CANCELED -> handleCanceledPayment(subscriptionPayment)
            else -> logger.info("결제 상태 변경 없음: merchant_uid=$merchantUid, status=$paymentStatus")
        }
    }

    /**
     * 성공한 결제 처리
     */
    private fun handleSuccessfulPayment(subscriptionPayment: SubscriptionPayment) {
        val adminId = subscriptionPayment.adminId

        // 구독 상태를 ACTIVE로 변경 (비활성 상태였다면)
        val subscription = subscriptionRepository.findByAdminIdAndStatus(adminId, SubscriptionStatus.ACTIVE)
            ?: subscriptionRepository.findByAdminId(adminId)?.firstOrNull()

        if (subscription != null && subscription.status != SubscriptionStatus.ACTIVE) {
            val activatedSubscription = subscription.copy(
                status = SubscriptionStatus.ACTIVE,
                updatedAt = LocalDateTime.now()
            )
            subscriptionRepository.save(activatedSubscription)
            logger.info("구독 활성화: adminId=$adminId")
        }

        // 스케줄이 있다면 상태 업데이트
        if (!subscriptionPayment.scheduleId.isNullOrBlank()) {
            val schedule = subscriptionScheduleRepository.findByScheduleId(subscriptionPayment.scheduleId)
            if (schedule != null && schedule.status == ScheduleStatus.SCHEDULED) {
                val updatedSchedule = schedule.copy(
                    lastPaymentDate = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
                subscriptionScheduleRepository.save(updatedSchedule)
            }
        }

        logger.info("성공한 구독 결제 처리 완료: adminId=$adminId, amount=${subscriptionPayment.amount}")
    }

    /**
     * 실패한 결제 처리
     */
    private fun handleFailedPayment(subscriptionPayment: SubscriptionPayment) {
        val adminId = subscriptionPayment.adminId

        // 연속 실패 횟수 확인 (예: 3회 연속 실패 시 구독 일시정지)
        val recentFailedPayments = subscriptionPaymentRepository.findByAdminIdAndStatus(adminId, PaymentStatus.FAILED)
            .filter { it.paymentDate.isAfter(LocalDateTime.now().minusDays(30)) }
            .size

        if (recentFailedPayments >= 3) {
            // 구독 일시정지
            val subscription = subscriptionRepository.findByAdminIdAndStatus(adminId, SubscriptionStatus.ACTIVE)
            if (subscription != null) {
                val suspendedSubscription = subscription.copy(
                    status = SubscriptionStatus.PAST_DUE,
                    updatedAt = LocalDateTime.now()
                )
                subscriptionRepository.save(suspendedSubscription)
                logger.warn("구독 일시정지 (연속 결제 실패): adminId=$adminId, 실패 횟수=$recentFailedPayments")
            }

            // 스케줄 일시정지
            if (!subscriptionPayment.scheduleId.isNullOrBlank()) {
                val schedule = subscriptionScheduleRepository.findByScheduleId(subscriptionPayment.scheduleId)
                if (schedule != null) {
                    val suspendedSchedule = schedule.copy(
                        status = ScheduleStatus.SUSPENDED,
                        updatedAt = LocalDateTime.now()
                    )
                    subscriptionScheduleRepository.save(suspendedSchedule)
                }
            }
        }

        logger.warn("실패한 구독 결제 처리: adminId=$adminId, 실패 사유=${subscriptionPayment.failureReason}")
    }

    /**
     * 취소된 결제 처리
     */
    private fun handleCanceledPayment(subscriptionPayment: SubscriptionPayment) {
        val adminId = subscriptionPayment.adminId
        logger.info("취소된 구독 결제 처리: adminId=$adminId, amount=${subscriptionPayment.amount}")
        
        // 필요에 따라 구독 상태 변경 로직 추가
        // 예: 부분 취소의 경우 환불 처리 등
    }

    /**
     * 웹훅 재시도 처리
     */
    fun retryFailedWebhook(impUid: String) = runBlocking {
        try {
            logger.info("웹훅 재시도: imp_uid=$impUid")
            
            val paymentInfo = portOneApiClient.getPaymentInfo(impUid)
            if (paymentInfo.code == 0 && paymentInfo.response != null) {
                val payment = paymentInfo.response
                val merchantUid = payment.get("merchant_uid")?.asText()
                
                if (!merchantUid.isNullOrBlank()) {
                    updatePaymentStatus(merchantUid, payment)
                    
                    if (merchantUid.startsWith("subscription_")) {
                        handleSubscriptionPayment(merchantUid, payment)
                    }
                    
                    logger.info("웹훅 재시도 성공: imp_uid=$impUid")
                } else {
                    logger.error("웹훅 재시도 실패 - merchant_uid 없음: imp_uid=$impUid")
                }
            } else {
                logger.error("웹훅 재시도 실패 - 결제 정보 조회 실패: imp_uid=$impUid")
            }
        } catch (e: Exception) {
            logger.error("웹훅 재시도 실패: imp_uid=$impUid", e)
            throw e
        }
    }
}