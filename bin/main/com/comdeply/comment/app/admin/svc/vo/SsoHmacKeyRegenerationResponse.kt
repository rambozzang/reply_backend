package com.comdeply.comment.app.admin.svc.vo

/**
 * HMAC 키 재생성 응답 DTO
 */
data class SsoHmacKeyRegenerationResponse(
    val siteId: Long,
    val newSecretKey: String,
    val warning: String,
    val regeneratedAt: Long
)
