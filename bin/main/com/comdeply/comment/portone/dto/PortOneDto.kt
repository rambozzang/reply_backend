package com.comdeply.comment.portone.dto

import java.math.BigDecimal
import java.time.LocalDateTime

// 토큰 관련 DTO
data class TokenRequest(
    val imp_key: String,
    val imp_secret: String
)

data class TokenResponse(
    val code: Int,
    val message: String?,
    val response: TokenData?
)

data class TokenData(
    val access_token: String,
    val now: Long,
    val expired_at: Long
)

// 빌링키 관련 DTO
data class BillingKeyRequest(
    val customer_uid: String,
    val card_number: String,
    val expiry: String,
    val birth: String,
    val pwd_2digit: String
)

data class BillingKeyResponse(
    val code: Int,
    val message: String?,
    val response: BillingKeyData?
)

data class BillingKeyData(
    val customer_uid: String,
    val card_name: String?,
    val card_number: String?,
    val card_type: String?,
    val bank: String?
)

data class BillingKeyInfoResponse(
    val code: Int,
    val message: String?,
    val response: BillingKeyData?
)

data class BillingKeyDeleteResponse(
    val code: Int,
    val message: String?
)

// 구독 결제 관련 DTO
data class SubscriptionPaymentRequest(
    val customer_uid: String,
    val merchant_uid: String,
    val amount: BigDecimal,
    val name: String,
    val buyer_name: String?,
    val buyer_email: String?
)

data class SubscriptionPaymentResponse(
    val code: Int,
    val message: String?,
    val response: PaymentData?
)

data class PaymentData(
    val imp_uid: String,
    val merchant_uid: String,
    val amount: BigDecimal,
    val status: String,
    val paid_at: Long?,
    val fail_reason: String?,
    val receipt_url: String?
)

data class PaymentInfoResponse(
    val code: Int,
    val message: String?,
    val response: PaymentData?
)

// 결제 취소 관련 DTO
data class PaymentCancelRequest(
    val imp_uid: String,
    val amount: BigDecimal?,
    val reason: String,
    val refund_holder: String?,
    val refund_bank: String?,
    val refund_account: String?
)

data class PaymentCancelResponse(
    val code: Int,
    val message: String?,
    val response: PaymentData?
)

// 에러 응답
data class PortOneErrorResponse(
    val code: Int,
    val message: String
)

// 웹훅 데이터
data class WebhookData(
    val imp_uid: String,
    val merchant_uid: String,
    val status: String
)