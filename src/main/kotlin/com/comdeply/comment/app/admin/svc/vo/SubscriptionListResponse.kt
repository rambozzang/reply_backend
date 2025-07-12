package com.comdeply.comment.app.admin.svc.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "구독 목록 응답")
data class SubscriptionListResponse(
    @Schema(description = "구독 ID", example = "1")
    val id: Long,

    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,

    @Schema(description = "사용자 정보")
    val user: UserInfo?,

    @Schema(description = "플랜 ID", example = "pro")
    val planId: String,

    @Schema(description = "플랜 이름", example = "Pro 플랜")
    val planName: String,

    @Schema(description = "월 결제 금액", example = "29000")
    val price: Int,

    @Schema(description = "월 댓글 한도", example = "50000")
    val monthlyCommentLimit: Int,

    @Schema(description = "구독 상태", example = "active")
    val status: String,

    @Schema(description = "구독 시작일", example = "2024-01-01T00:00:00")
    val startDate: String,

    @Schema(description = "다음 결제일", example = "2024-02-01T00:00:00")
    val nextBillingDate: String?,

    @Schema(description = "취소일", example = "2024-01-15T00:00:00")
    val cancelledAt: String?,

    @Schema(description = "취소 사유", example = "사용자 요청")
    val cancelReason: String?,

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
