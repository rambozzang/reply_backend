package com.comdeply.comment.app.admin.cntr

import com.comdeply.comment.app.admin.svc.AdminPaymentService
import com.comdeply.comment.app.admin.svc.AdminService
import com.comdeply.comment.app.admin.svc.vo.PaymentDetailResponse
import com.comdeply.comment.app.admin.svc.vo.PaymentStatsResponse
import com.comdeply.comment.common.PageResponse
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.ApiResponse
import com.comdeply.comment.dto.PaymentResponse
import com.comdeply.comment.entity.PaymentStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/payments")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "관리자 - 결제 관리", description = "관리자용 결제 관리 API")
class AdminPaymentController(
    private val adminService: AdminService,
    private val adminPaymentService: AdminPaymentService
) {
    private val logger = LoggerFactory.getLogger(AdminPaymentController::class.java)

    @GetMapping
    @Operation(summary = "결제 내역 조회", description = "관리자가 관리하는 사이트의 결제 내역을 조회합니다")
    fun getPayments(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: PaymentStatus?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?
    ): ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> {
        logger.info("결제 내역 조회 요청: page={}, size={}, status={}", page, size, status)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val paymentsPage = adminPaymentService.getPaymentsForAdmin(
                admin = currentAdmin,
                page = page,
                size = size,
                status = status,
                startDate = startDate,
                endDate = endDate
            )

            val pageResponse = PageResponse.of(paymentsPage)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = pageResponse,
                    message = "결제 내역을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("결제 내역 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "결제 내역 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("결제 내역 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("결제 내역 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "결제 통계 조회", description = "관리자가 관리하는 사이트의 결제 통계를 조회합니다")
    fun getPaymentStats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<PaymentStatsResponse>> {
        logger.info("결제 통계 조회 요청")

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val stats = adminPaymentService.getPaymentStats(currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = stats,
                    message = "결제 통계를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("결제 통계 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "결제 통계 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("결제 통계 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("결제 통계 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "결제 상세 조회", description = "특정 결제의 상세 정보를 조회합니다")
    fun getPaymentDetail(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable paymentId: Long
    ): ResponseEntity<ApiResponse<PaymentDetailResponse>> {
        logger.info("결제 상세 조회 요청: paymentId={}", paymentId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val paymentDetail = adminPaymentService.getPaymentDetail(paymentId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = paymentDetail,
                    message = "결제 상세 정보를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("결제 상세 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "결제 상세 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("결제 상세 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("결제 상세 조회에 실패했습니다")
            )
        }
    }
}
