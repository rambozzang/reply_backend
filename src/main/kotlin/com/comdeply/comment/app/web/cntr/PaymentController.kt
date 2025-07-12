package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.PaymentService
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/payments")
@Tag(name = "결제 API", description = "사용자용 결제 관련 API")
class PaymentController(
    private val paymentService: PaymentService
) {

    private val logger = LoggerFactory.getLogger(PaymentController::class.java)

    @Operation(summary = "결제 생성", description = "새로운 결제를 생성합니다")
    @PostMapping
    fun createPayment(
        @Parameter(description = "인증된 사용자 정보", hidden = true)
        @AuthenticationPrincipal
        userPrincipal: UserPrincipal,
        @RequestBody @Valid
        request: Map<String, Any>
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return try {
            val paymentId = request["paymentId"] as? String
                ?: throw IllegalArgumentException("결제 ID는 필수입니다")
            val planId = request["planId"] as? String
                ?: throw IllegalArgumentException("플랜 ID는 필수입니다")
            val planName = request["planName"] as? String
                ?: throw IllegalArgumentException("플랜명은 필수입니다")
            val amount = (request["amount"] as? Number)?.toInt()
                ?: throw IllegalArgumentException("결제 금액은 필수입니다")

            val payment = paymentService.createPayment(
                adminId = userPrincipal.id,
                paymentId = paymentId,
                planId = planId,
                planName = planName,
                amount = amount
            )

            logger.info("결제 생성 성공: 사용자=${userPrincipal.id}, 결제ID=$paymentId")

            ResponseEntity.ok(
                ApiResponse.success(
                    mapOf(
                        "paymentId" to payment.paymentId,
                        "status" to payment.status.name.lowercase(),
                        "message" to "결제가 생성되었습니다"
                    )
                )
            )
        } catch (e: Exception) {
            logger.error("결제 생성 실패: 사용자=${userPrincipal.id}", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<Map<String, Any>>("결제 생성에 실패했습니다: ${e.message}")
            )
        }
    }

    @Operation(summary = "결제 검증", description = "결제 완료 후 검증을 수행합니다")
    @PostMapping("/verify")
    fun verifyPayment(
        @Parameter(description = "인증된 사용자 정보", hidden = true)
        @AuthenticationPrincipal
        userPrincipal: UserPrincipal,
        @RequestBody @Valid
        request: PaymentVerificationRequest
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return try {
            val result = runBlocking {
                paymentService.verifyPayment(request.paymentId)
            }

            logger.info("결제 검증 완료: 사용자=${userPrincipal.id}, 결제ID=${request.paymentId}, 성공=${result["success"]}")

            ResponseEntity.ok(ApiResponse.success(result))
        } catch (e: Exception) {
            logger.error("결제 검증 실패: 사용자=${userPrincipal.id}, 결제ID=${request.paymentId}", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<Map<String, Any>>("결제 검증에 실패했습니다: ${e.message}")
            )
        }
    }

    @Operation(summary = "결제 상세 정보 조회", description = "특정 결제의 상세 정보를 조회합니다")
    @GetMapping("/{paymentId}")
    fun getPaymentDetail(
        @Parameter(description = "인증된 사용자 정보", hidden = true)
        @AuthenticationPrincipal
        userPrincipal: UserPrincipal,
        @Parameter(description = "결제 ID")
        @PathVariable
        paymentId: String
    ): ResponseEntity<ApiResponse<PaymentDetailResponse>> {
        return try {
            val paymentDetail = paymentService.getPaymentDetail(paymentId)

            logger.info("결제 상세 조회 성공: 사용자=${userPrincipal.id}, 결제ID=$paymentId")

            ResponseEntity.ok(ApiResponse.success(paymentDetail))
        } catch (e: Exception) {
            logger.error("결제 상세 조회 실패: 사용자=${userPrincipal.id}, 결제ID=$paymentId", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error("결제 정보를 조회할 수 없습니다: ${e.message}")
            )
        }
    }

    @Operation(summary = "결제 내역 조회", description = "사용자의 결제 내역을 조회합니다")
    @GetMapping("/history")
    fun getPaymentHistory(
        @Parameter(description = "인증된 사용자 정보", hidden = true)
        @AuthenticationPrincipal
        userPrincipal: UserPrincipal,
        @Parameter(description = "페이지 번호 (1부터 시작)")
        @RequestParam(defaultValue = "1")
        page: Int,
        @Parameter(description = "페이지 크기")
        @RequestParam(defaultValue = "10")
        size: Int
    ): ResponseEntity<ApiResponse<PaymentHistoryResponse>> {
        return try {
            val history = paymentService.getPaymentHistory(userPrincipal.id, page, size)

            logger.info("결제 내역 조회 성공: 사용자=${userPrincipal.id}, 페이지=$page, 크기=$size")

            ResponseEntity.ok(ApiResponse.success(history))
        } catch (e: Exception) {
            logger.error("결제 내역 조회 실패: 사용자=${userPrincipal.id}", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error("결제 내역을 조회할 수 없습니다: ${e.message}")
            )
        }
    }

    @Operation(summary = "결제 환불", description = "결제를 환불 처리합니다")
    @PostMapping("/{paymentId}/refund")
    fun requestRefund(
        @Parameter(description = "인증된 사용자 정보", hidden = true)
        @AuthenticationPrincipal
        userPrincipal: UserPrincipal,
        @Parameter(description = "결제 ID")
        @PathVariable
        paymentId: String,
        @RequestBody @Valid
        request: RefundRequest
    ): ResponseEntity<ApiResponse<RefundResponse>> {
        return try {
            val refundResponse = runBlocking {
                paymentService.requestRefund(paymentId, request.reason)
            }

            logger.info("환불 요청 처리: 사용자=${userPrincipal.id}, 결제ID=$paymentId, 성공=${refundResponse.success}")

            if (refundResponse.success) {
                ResponseEntity.ok(ApiResponse.success(refundResponse))
            } else {
                ResponseEntity.badRequest().body(
                    ApiResponse.error<RefundResponse>(refundResponse.message ?: "환불 처리에 실패했습니다")
                )
            }
        } catch (e: Exception) {
            logger.error("환불 요청 실패: 사용자=${userPrincipal.id}, 결제ID=$paymentId", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<RefundResponse>("환불 요청에 실패했습니다: ${e.message}")
            )
        }
    }

    @Operation(summary = "결제 상태 조회", description = "결제 상태를 조회합니다 (콜백 페이지용)")
    @GetMapping("/status/{paymentId}")
    fun getPaymentStatus(
        @Parameter(description = "결제 ID")
        @PathVariable
        paymentId: String
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return try {
            val paymentDetail = paymentService.getPaymentDetail(paymentId)

            val response: Map<String, Any> = mapOf(
                "paymentId" to paymentDetail.paymentId,
                "status" to paymentDetail.status,
                "planName" to paymentDetail.planName,
                "amount" to paymentDetail.amount,
                "paidAt" to (paymentDetail.paidAt ?: ""),
                "message" to (paymentDetail.message ?: ""),
                "success" to (paymentDetail.status == "paid")
            )

            logger.info("결제 상태 조회 성공: 결제ID=$paymentId, 상태=${paymentDetail.status}")

            ResponseEntity.ok(ApiResponse.success(response))
        } catch (e: Exception) {
            logger.error("결제 상태 조회 실패: 결제ID=$paymentId", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<Map<String, Any>>("결제 상태를 조회할 수 없습니다: ${e.message}")
            )
        }
    }
}
