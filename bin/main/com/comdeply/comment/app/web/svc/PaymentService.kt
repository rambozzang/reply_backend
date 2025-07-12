package com.comdeply.comment.app.web.svc

import com.comdeply.comment.dto.PaymentDetailResponse
import com.comdeply.comment.dto.PaymentHistoryResponse
import com.comdeply.comment.dto.PaymentSummary
import com.comdeply.comment.dto.RefundResponse
import com.comdeply.comment.entity.Payment
import com.comdeply.comment.entity.PaymentStatus
import com.comdeply.comment.portone.service.PortOneApiClient
import com.comdeply.comment.repository.AdminRepository
import com.comdeply.comment.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val subscriptionService: SubscriptionService,
    private val adminRepository: AdminRepository,
    private val portOneApiClient: PortOneApiClient
) {

    private val logger = LoggerFactory.getLogger(PaymentService::class.java)

    /**
     * 결제 생성 (관리자가 결제 시작 시 호출)
     */
    fun createPayment(
        adminId: Long,
        paymentId: String,
        planId: String,
        planName: String,
        amount: Int
    ): Payment {
        val admin = adminRepository.findById(adminId).orElse(null)
            ?: throw IllegalArgumentException("관리자를 찾을 수 없습니다: $adminId")

        // 중복 결제 ID 확인
        paymentRepository.findByPaymentId(paymentId)?.let {
            throw IllegalArgumentException("이미 존재하는 결제 ID입니다: $paymentId")
        }

        val payment = Payment(
            admin = admin,
            paymentId = paymentId,
            planId = planId,
            planName = planName,
            amount = amount,
            status = PaymentStatus.PENDING
        )

        return paymentRepository.save(payment).also {
            logger.info("결제 생성: 관리자=$adminId, 결제ID=$paymentId, 플랜=$planId, 금액=$amount")
        }
    }

    /**
     * 결제 검증 (프론트엔드에서 결제 완료 후 호출)
     */
    suspend fun verifyPayment(paymentId: String): Map<String, Any> {
        logger.info("결제 검증 시작: $paymentId")

        // 데이터베이스에서 결제 정보 조회
        val payment = paymentRepository.findByPaymentId(paymentId)
            ?: throw IllegalArgumentException("결제 정보를 찾을 수 없습니다: $paymentId")

        // 포트원에서 결제 정보 조회
        val portOnePayment = kotlinx.coroutines.runBlocking {
            portOneApiClient.getPaymentInfo(paymentId)
        }

        // 결제 상태 업데이트
        val updatedPayment = when (portOnePayment.response?.status?.uppercase()) {
            "PAID" -> {
                payment.copy(
                    status = PaymentStatus.PAID,
                    paidAt = LocalDateTime.now(),
                    portoneTransactionId = portOnePayment.response?.impUid,
                    paymentMethod = portOnePayment.response?.payMethod,
                    cardCompany = portOnePayment.response?.cardName,
                    cardNumber = portOnePayment.response?.cardNumber,
                    updatedAt = LocalDateTime.now()
                )
            }
            "FAILED" -> {
                payment.copy(
                    status = PaymentStatus.FAILED,
                    failureReason = "결제 실패",
                    updatedAt = LocalDateTime.now()
                )
            }
            "CANCELLED" -> {
                payment.copy(
                    status = PaymentStatus.CANCELLED,
                    cancelledAt = LocalDateTime.now(),
                    cancelReason = "결제 취소",
                    updatedAt = LocalDateTime.now()
                )
            }
            else -> payment
        }

        paymentRepository.save(updatedPayment)

        // 결제 성공 시 구독 생성
        val isSuccess = portOnePayment.response?.status?.uppercase() == "PAID"
        if (isSuccess) {
            subscriptionService.createSubscription(
                admin = payment.admin,
                planId = payment.planId,
                planName = payment.planName,
                amount = payment.amount
            )
            logger.info("결제 성공 및 구독 생성 완료: $paymentId")
        }

        return mapOf(
            "success" to isSuccess,
            "status" to (portOnePayment.response?.status ?: "UNKNOWN"),
            "message" to if (isSuccess) "결제가 완료되었습니다" else "결제에 실패했습니다"
        )
    }

    /**
     * 결제 상세 정보 조회
     */
    fun getPaymentDetail(paymentId: String): PaymentDetailResponse {
        val payment = paymentRepository.findByPaymentId(paymentId)
            ?: throw IllegalArgumentException("결제 정보를 찾을 수 없습니다: $paymentId")

        return PaymentDetailResponse(
            paymentId = payment.paymentId,
            planName = payment.planName,
            amount = payment.amount,
            status = payment.status.name.lowercase(),
            paidAt = payment.paidAt,
            message = when (payment.status) {
                PaymentStatus.PAID -> "결제가 완료되었습니다"
                PaymentStatus.FAILED -> payment.failureReason ?: "결제에 실패했습니다"
                PaymentStatus.CANCELLED -> payment.cancelReason ?: "결제가 취소되었습니다"
                PaymentStatus.PENDING -> "결제 처리 중입니다"
                PaymentStatus.REFUNDED -> "결제가 환불되었습니다"
            },
            errorCode = if (payment.status == PaymentStatus.FAILED) "PAYMENT_FAILED" else null
        )
    }

    /**
     * 관리자별 결제 내역 조회
     */
    fun getPaymentHistory(adminId: Long, page: Int, size: Int): PaymentHistoryResponse {
        val pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending())
        val paymentsPage = paymentRepository.findByAdminIdOrderByCreatedAtDesc(adminId, pageable)

        val payments = paymentsPage.content.map { payment ->
            PaymentSummary(
                id = payment.id,
                paymentId = payment.paymentId,
                planName = payment.planName,
                amount = payment.amount,
                status = payment.status.name.lowercase(),
                paymentMethod = payment.paymentMethod,
                cardCompany = payment.cardCompany,
                createdAt = payment.createdAt,
                paidAt = payment.paidAt
            )
        }

        return PaymentHistoryResponse(
            payments = payments,
            totalPages = paymentsPage.totalPages,
            currentPage = page,
            totalElements = paymentsPage.totalElements
        )
    }

    /**
     * 결제 환불 처리
     */
    suspend fun requestRefund(paymentId: String, reason: String): RefundResponse {
        logger.info("환불 요청 시작: $paymentId, 사유: $reason")

        val payment = paymentRepository.findByPaymentId(paymentId)
            ?: throw IllegalArgumentException("결제 정보를 찾을 수 없습니다: $paymentId")

        if (payment.status != PaymentStatus.PAID) {
            throw IllegalStateException("환불 가능한 상태가 아닙니다: ${payment.status}")
        }

        return try {
            // 포트원 환불 요청
            val cancelRequest = com.comdeply.comment.portone.dto.PaymentCancelRequest(
                imp_uid = payment.portoneTransactionId ?: "",
                amount = java.math.BigDecimal(payment.amount.toLong()),
                reason = reason,
                refund_holder = "", // TODO: 환불 계좌 정보
                refund_bank = "",   // TODO: 환불 은행 정보  
                refund_account = "" // TODO: 환불 계좌번호
            )
            val cancelResponse = kotlinx.coroutines.runBlocking {
                portOneApiClient.cancelPayment(cancelRequest)
            }

            // 결제 상태 업데이트
            val updatedPayment = payment.copy(
                status = PaymentStatus.REFUNDED,
                refundedAt = LocalDateTime.now(),
                cancelReason = reason,
                updatedAt = LocalDateTime.now()
            )
            paymentRepository.save(updatedPayment)

            // 구독 취소
            subscriptionService.cancelSubscriptionByAdminId(payment.admin.id)

            RefundResponse(
                success = cancelResponse.code == 0,
                cancellationId = cancelResponse.response?.get("imp_uid")?.asText(),
                message = cancelResponse.message ?: "환불이 완료되었습니다"
            )
        } catch (e: Exception) {
            logger.error("환불 처리 중 오류 발생: $paymentId", e)
            RefundResponse(
                success = false,
                cancellationId = null,
                message = "환불 처리 중 오류가 발생했습니다: ${e.message}"
            )
        }
    }
}
