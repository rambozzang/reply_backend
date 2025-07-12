package com.comdeply.comment.dto

import com.comdeply.comment.entity.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

// 결제 응답 (관리자용)
@Schema(description = "결제 응답")
data class PaymentResponse(
    @Schema(description = "결제 ID", example = "1")
    val id: Long,
    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,
    @Schema(description = "결제 금액", example = "29000")
    val amount: Long,
    @Schema(description = "통화", example = "KRW")
    val currency: String,
    @Schema(description = "결제 상태", example = "PAID")
    val status: PaymentStatus,
    @Schema(description = "결제 방법", example = "카드")
    val paymentMethod: String?,
    @Schema(description = "거래 ID", example = "transaction_123")
    val transactionId: String?,
    @Schema(description = "생성일", example = "2024-01-01T12:00:00")
    val createdAt: LocalDateTime,
    @Schema(description = "수정일", example = "2024-01-01T12:00:00")
    val updatedAt: LocalDateTime,
    @Schema(description = "설명", example = "Pro 플랜 결제")
    val description: String?
)

// 결제 검증 요청
@Schema(description = "결제 검증 요청")
data class PaymentVerificationRequest(
    @Schema(description = "결제 ID", example = "payment_1234567890")
    @field:NotBlank(message = "결제 ID는 필수입니다")
    val paymentId: String
)

// 결제 상세 응답
@Schema(description = "결제 상세 응답")
data class PaymentDetailResponse(
    @Schema(description = "결제 ID", example = "payment_1234567890")
    val paymentId: String,
    @Schema(description = "플랜명", example = "Pro 플랜")
    val planName: String,
    @Schema(description = "결제 금액", example = "29000")
    val amount: Int,
    @Schema(description = "결제 상태", example = "paid")
    val status: String,
    @Schema(description = "결제일", example = "2024-01-01T12:00:00")
    val paidAt: LocalDateTime?,
    @Schema(description = "메시지", example = "결제가 완료되었습니다")
    val message: String?,
    @Schema(description = "오류 코드", example = "null")
    val errorCode: String?
)

// 결제 내역 응답
@Schema(description = "결제 내역 응답")
data class PaymentHistoryResponse(
    @Schema(description = "결제 목록")
    val payments: List<PaymentSummary>,
    @Schema(description = "전체 페이지 수", example = "5")
    val totalPages: Int,
    @Schema(description = "현재 페이지", example = "1")
    val currentPage: Int,
    @Schema(description = "전체 항목 수", example = "50")
    val totalElements: Long
)

// 결제 요약 정보
@Schema(description = "결제 요약 정보")
data class PaymentSummary(
    @Schema(description = "결제 ID", example = "1")
    val id: Long,
    @Schema(description = "포트원 결제 ID", example = "payment_1234567890")
    val paymentId: String,
    @Schema(description = "플랜명", example = "Pro 플랜")
    val planName: String,
    @Schema(description = "결제 금액", example = "29000")
    val amount: Int,
    @Schema(description = "결제 상태", example = "paid")
    val status: String,
    @Schema(description = "결제 방법", example = "카드")
    val paymentMethod: String?,
    @Schema(description = "카드사", example = "신한카드")
    val cardCompany: String?,
    @Schema(description = "생성일", example = "2024-01-01T12:00:00")
    val createdAt: LocalDateTime,
    @Schema(description = "결제일", example = "2024-01-01T12:00:00")
    val paidAt: LocalDateTime?
)

// 환불 요청
@Schema(description = "환불 요청")
data class RefundRequest(
    @Schema(description = "환불 사유", example = "고객 요청")
    @field:NotBlank(message = "환불 사유는 필수입니다")
    val reason: String
)

// 환불 응답
@Schema(description = "환불 응답")
data class RefundResponse(
    @Schema(description = "성공 여부", example = "true")
    val success: Boolean,
    @Schema(description = "취소 ID", example = "cancellation_1234567890")
    val cancellationId: String?,
    @Schema(description = "메시지", example = "환불이 완료되었습니다")
    val message: String?
)

// 구독 정보 응답
@Schema(description = "구독 정보 응답")
data class SubscriptionResponse(
    @Schema(description = "구독 ID", example = "1")
    val id: Long,
    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,
    @Schema(description = "플랜 ID", example = "pro")
    val planId: String,
    @Schema(description = "플랜명", example = "Pro 플랜")
    val planName: String,
    @Schema(description = "월 결제 금액", example = "29000")
    val price: Int,
    @Schema(description = "월 댓글 한도", example = "50000")
    val monthlyCommentLimit: Int,
    @Schema(description = "현재 댓글 수", example = "1250")
    val currentCommentCount: Int,
    @Schema(description = "구독 상태", example = "active")
    val status: String,
    @Schema(description = "시작일", example = "2024-01-01T12:00:00")
    val startDate: String,
    @Schema(description = "다음 결제일", example = "2024-02-01T12:00:00")
    val nextBillingDate: String?,
    @Schema(description = "취소일", example = "2024-01-15T12:00:00")
    val cancelledAt: String?,
    @Schema(description = "취소 사유", example = "사용자 요청")
    val cancelReason: String?,
    @Schema(description = "생성일", example = "2024-01-01T12:00:00")
    val createdAt: String,
    @Schema(description = "수정일", example = "2024-01-01T12:00:00")
    val updatedAt: String
)

// 구독 취소 요청
@Schema(description = "구독 취소 요청")
data class SubscriptionCancelRequest(
    @Schema(description = "취소 사유", example = "서비스 불만족")
    val reason: String? = null
)

// 포트원 결제 응답 (내부 사용)
data class PortOnePaymentResponse(
    val id: String,
    val status: String,
    val amount: PortOneAmount,
    val paidAt: String?,
    val customer: PortOneCustomer?,
    val customData: Map<String, Any>?,
    val paymentMethod: PortOnePaymentMethod?
)

data class PortOneAmount(
    val total: Int,
    val currency: String
)

data class PortOneCustomer(
    val id: String?,
    val name: String?,
    val email: String?
)

data class PortOnePaymentMethod(
    val type: String?,
    val card: PortOneCard?
)

data class PortOneCard(
    val company: String?,
    val number: String?
)

// 포트원 취소 응답 (내부 사용)
data class PortOneCancelResponse(
    val cancellation: PortOneCancellation
)

data class PortOneCancellation(
    val id: String,
    val status: String,
    val cancelledAt: String,
    val reason: String?
)

// 웹훅 데이터 (내부 사용)
data class WebhookData(
    val type: String,
    val data: WebhookPaymentData
)

data class WebhookPaymentData(
    val paymentId: String,
    val status: String?,
    val amount: PortOneAmount?
)
