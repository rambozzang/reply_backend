package com.comdeply.comment.app.admin.svc.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "결제 목록 응답")
data class PaymentListResponse(
    @Schema(description = "결제 ID", example = "1")
    val id: Long,

    @Schema(description = "결제 ID (외부)", example = "payment_1234567890")
    val paymentId: String,

    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,

    @Schema(description = "사용자 정보")
    val user: UserInfo?,

    @Schema(description = "플랜 ID", example = "pro")
    val planId: String,

    @Schema(description = "플랜 이름", example = "Pro 플랜")
    val planName: String,

    @Schema(description = "결제 금액", example = "29000")
    val amount: Int,

    @Schema(description = "결제 상태", example = "PAID")
    val status: String,

    @Schema(description = "결제 방법", example = "CARD")
    val paymentMethod: String?,

    @Schema(description = "결제 완료일", example = "2024-01-01T12:00:00")
    val paidAt: String?,

    @Schema(description = "실패 사유", example = "카드 한도 초과")
    val failureReason: String?,

    @Schema(description = "환불일", example = "2024-01-15T12:00:00")
    val refundedAt: String?,

    @Schema(description = "환불 사유", example = "사용자 요청")
    val refundReason: String?,

    @Schema(description = "생성일", example = "2024-01-01T00:00:00")
    val createdAt: String,

    @Schema(description = "수정일", example = "2024-01-01T00:00:00")
    val updatedAt: String
) {
    @Schema(description = "사용자 정보")
    data class UserInfo(
        @Schema(description = "사용자 ID", example = "1")
        val id: Long,

        @Schema(description = "사용자 이름", example = "홍길동")
        val name: String,

        @Schema(description = "사용자 이메일", example = "user@example.com")
        val email: String
    )
}
