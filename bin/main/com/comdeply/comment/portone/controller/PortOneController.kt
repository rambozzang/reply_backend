package com.comdeply.comment.portone.controller

import com.comdeply.comment.app.web.entity.SubscriptionPlan
import com.comdeply.comment.portone.dto.BillingCycle
import com.comdeply.comment.portone.service.PortOneBillingService
import com.comdeply.comment.portone.service.PortOneSubscriptionService
import com.comdeply.comment.portone.service.PortOneWebhookService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/portone")
class PortOneController(
    private val portOneBillingService: PortOneBillingService,
    private val portOneSubscriptionService: PortOneSubscriptionService,
    private val portOneWebhookService: PortOneWebhookService
) {
    
    private val logger = LoggerFactory.getLogger(PortOneController::class.java)

    /**
     * 빌링키 발급
     */
    @PostMapping("/billing-key")
    fun issueBillingKey(
        @RequestParam adminId: Long,
        @RequestParam cardNumber: String,
        @RequestParam expiry: String,
        @RequestParam birth: String,
        @RequestParam pwd2digit: String
    ): ResponseEntity<Any> {
        return try {
            val billingKey = portOneBillingService.issueBillingKey(adminId, cardNumber, expiry, birth, pwd2digit)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "빌링키 발급 성공",
                "billingKey" to mapOf(
                    "id" to billingKey.id,
                    "cardName" to billingKey.cardName,
                    "cardNumber" to billingKey.cardNumber,
                    "cardType" to billingKey.cardType,
                    "bank" to billingKey.bank
                )
            ))
        } catch (e: Exception) {
            logger.error("빌링키 발급 실패", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to (e.message ?: "빌링키 발급에 실패했습니다.")
            ))
        }
    }

    /**
     * 빌링키 조회
     */
    @GetMapping("/billing-key/{adminId}")
    fun getBillingKey(@PathVariable adminId: Long): ResponseEntity<Any> {
        val billingKey = portOneBillingService.getBillingKey(adminId)
        
        return if (billingKey != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "billingKey" to mapOf(
                    "id" to billingKey.id,
                    "cardName" to billingKey.cardName,
                    "cardNumber" to billingKey.cardNumber,
                    "cardType" to billingKey.cardType,
                    "bank" to billingKey.bank,
                    "status" to billingKey.status
                )
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "billingKey" to null,
                "message" to "등록된 빌링키가 없습니다."
            ))
        }
    }

    /**
     * 빌링키 삭제
     */
    @DeleteMapping("/billing-key/{adminId}")
    fun deleteBillingKey(@PathVariable adminId: Long): ResponseEntity<Any> {
        return try {
            val result = portOneBillingService.deleteBillingKey(adminId)
            if (result) {
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to "빌링키 삭제 성공"
                ))
            } else {
                ResponseEntity.badRequest().body(mapOf(
                    "success" to false,
                    "message" to "빌링키 삭제 실패"
                ))
            }
        } catch (e: Exception) {
            logger.error("빌링키 삭제 실패", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to (e.message ?: "빌링키 삭제에 실패했습니다.")
            ))
        }
    }

    /**
     * 구독 시작
     */
    @PostMapping("/subscription/start")
    fun startSubscription(
        @RequestParam adminId: Long,
        @RequestParam plan: String,
        @RequestParam(defaultValue = "MONTHLY") billingCycle: String
    ): ResponseEntity<Any> {
        return try {
            val subscriptionPlan = SubscriptionPlan.valueOf(plan.uppercase())
            val cycle = BillingCycle.valueOf(billingCycle.uppercase())
            
            val subscription = portOneSubscriptionService.startSubscription(adminId, subscriptionPlan, cycle)
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "구독 시작 성공",
                "subscription" to mapOf(
                    "id" to subscription.id,
                    "plan" to subscription.plan,
                    "status" to subscription.status,
                    "startDate" to subscription.startDate,
                    "nextBillingDate" to subscription.nextBillingDate,
                    "billingCycle" to subscription.billingCycle
                )
            ))
        } catch (e: Exception) {
            logger.error("구독 시작 실패", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to (e.message ?: "구독 시작에 실패했습니다.")
            ))
        }
    }

    /**
     * 구독 취소
     */
    @PostMapping("/subscription/cancel/{adminId}")
    fun cancelSubscription(@PathVariable adminId: Long): ResponseEntity<Any> {
        return try {
            val result = portOneSubscriptionService.cancelSubscription(adminId)
            if (result) {
                ResponseEntity.ok(mapOf(
                    "success" to true,
                    "message" to "구독 취소 성공"
                ))
            } else {
                ResponseEntity.badRequest().body(mapOf(
                    "success" to false,
                    "message" to "취소할 구독이 없거나 취소에 실패했습니다."
                ))
            }
        } catch (e: Exception) {
            logger.error("구독 취소 실패", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to (e.message ?: "구독 취소에 실패했습니다.")
            ))
        }
    }

    /**
     * 구독 플랜 변경
     */
    @PostMapping("/subscription/change-plan")
    fun changePlan(
        @RequestParam adminId: Long,
        @RequestParam newPlan: String
    ): ResponseEntity<Any> {
        return try {
            val subscriptionPlan = SubscriptionPlan.valueOf(newPlan.uppercase())
            val subscription = portOneSubscriptionService.changePlan(adminId, subscriptionPlan)
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "플랜 변경 성공",
                "subscription" to mapOf(
                    "id" to subscription.id,
                    "plan" to subscription.plan,
                    "status" to subscription.status,
                    "nextBillingDate" to subscription.nextBillingDate
                )
            ))
        } catch (e: Exception) {
            logger.error("플랜 변경 실패", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to (e.message ?: "플랜 변경에 실패했습니다.")
            ))
        }
    }

    /**
     * 구독 정보 조회
     */
    @GetMapping("/subscription/{adminId}")
    fun getSubscription(@PathVariable adminId: Long): ResponseEntity<Any> {
        val subscription = portOneSubscriptionService.getSubscription(adminId)
        
        return if (subscription != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "subscription" to mapOf(
                    "id" to subscription.id,
                    "plan" to subscription.plan,
                    "status" to subscription.status,
                    "startDate" to subscription.startDate,
                    "endDate" to subscription.endDate,
                    "nextBillingDate" to subscription.nextBillingDate,
                    "billingCycle" to subscription.billingCycle
                )
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "subscription" to null,
                "message" to "활성 구독이 없습니다."
            ))
        }
    }

    /**
     * 결제 내역 조회
     */
    @GetMapping("/payments/{adminId}")
    fun getPaymentHistory(@PathVariable adminId: Long): ResponseEntity<Any> {
        return try {
            val payments = portOneSubscriptionService.getPaymentHistory(adminId)
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "payments" to payments.map { payment ->
                    mapOf(
                        "id" to payment.id,
                        "paymentId" to payment.paymentId,
                        "amount" to payment.amount,
                        "status" to payment.status,
                        "paymentDate" to payment.paymentDate,
                        "paidAt" to payment.paidAt,
                        "description" to payment.description,
                        "planType" to payment.planType,
                        "failureReason" to payment.failureReason
                    )
                }
            ))
        } catch (e: Exception) {
            logger.error("결제 내역 조회 실패", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to (e.message ?: "결제 내역 조회에 실패했습니다.")
            ))
        }
    }

    /**
     * PortOne 웹훅 엔드포인트
     */
    @PostMapping("/webhook")
    fun handleWebhook(
        @RequestBody payload: String,
        @RequestHeader("X-IamportSignature", required = false) signature: String?
    ): ResponseEntity<Any> {
        return try {
            portOneWebhookService.processWebhook(payload, signature)
            ResponseEntity.ok(mapOf("success" to true))
        } catch (e: Exception) {
            logger.error("웹훅 처리 실패", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to (e.message ?: "웹훅 처리에 실패했습니다.")
            ))
        }
    }

    /**
     * 빌링키 유효성 확인
     */
    @GetMapping("/billing-key/{adminId}/validate")
    fun validateBillingKey(@PathVariable adminId: Long): ResponseEntity<Any> {
        return try {
            val isValid = portOneBillingService.validateBillingKey(adminId)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "valid" to isValid,
                "message" to if (isValid) "빌링키가 유효합니다." else "빌링키가 유효하지 않습니다."
            ))
        } catch (e: Exception) {
            logger.error("빌링키 유효성 확인 실패", e)
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to (e.message ?: "빌링키 유효성 확인에 실패했습니다.")
            ))
        }
    }
}