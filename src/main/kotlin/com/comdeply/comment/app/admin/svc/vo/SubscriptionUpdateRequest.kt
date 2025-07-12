package com.comdeply.comment.app.admin.svc.vo

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern

@Schema(description = "구독 수정 요청")
data class SubscriptionUpdateRequest(
    @Schema(description = "플랜 ID", example = "pro")
    val planId: String?,

    @Schema(description = "플랜 이름", example = "Pro 플랜")
    val planName: String?,

    @Schema(description = "월 결제 금액", example = "29000")
    @field:Min(value = 0, message = "가격은 0 이상이어야 합니다")
    val price: Int?,

    @Schema(description = "월 댓글 한도", example = "50000")
    @field:Min(value = 0, message = "댓글 한도는 0 이상이어야 합니다")
    val monthlyCommentLimit: Int?,

    @Schema(description = "구독 상태", example = "active", allowableValues = ["active", "cancelled", "expired"])
    @field:Pattern(regexp = "^(active|cancelled|expired)$", message = "상태는 active, cancelled, expired 중 하나여야 합니다")
    val status: String?,

    @Schema(description = "다음 결제일", example = "2024-02-01T00:00:00")
    val nextBillingDate: String?
)
