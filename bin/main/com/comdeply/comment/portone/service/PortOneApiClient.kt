package com.comdeply.comment.portone.service

import com.comdeply.comment.portone.config.PortOneProperties
import com.comdeply.comment.portone.dto.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono
import java.math.BigDecimal

@Service
class PortOneApiClient(
    private val portOneProperties: PortOneProperties,
    private val objectMapper: ObjectMapper
) {
    
    private val logger = LoggerFactory.getLogger(PortOneApiClient::class.java)
    
    private val webClient = WebClient.builder()
        .baseUrl(portOneProperties.baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()
    
    private var accessToken: String? = null
    private var tokenExpiredAt: Long = 0

    /**
     * 액세스 토큰 발급
     */
    suspend fun getAccessToken(): String {
        // 토큰이 유효하면 재사용
        if (accessToken != null && System.currentTimeMillis() / 1000 < tokenExpiredAt - 60) {
            return accessToken!!
        }

        val tokenRequest = TokenRequest(
            imp_key = portOneProperties.apiKey,
            imp_secret = portOneProperties.apiSecret
        )

        try {
            val response = webClient.post()
                .uri("/users/getToken")
                .bodyValue(tokenRequest)
                .retrieve()
                .awaitBody<TokenResponse>()

            if (response.code == 0 && response.response != null) {
                accessToken = response.response.access_token
                tokenExpiredAt = response.response.expired_at
                logger.info("PortOne 액세스 토큰 발급 성공")
                return accessToken!!
            } else {
                throw RuntimeException("토큰 발급 실패: ${response.message}")
            }
        } catch (e: Exception) {
            logger.error("PortOne 토큰 발급 실패", e)
            throw RuntimeException("토큰 발급 실패: ${e.message}", e)
        }
    }

    /**
     * 빌링키 발급
     */
    suspend fun issueBillingKey(request: BillingKeyRequest): BillingKeyResponse {
        val token = getAccessToken()
        
        try {
            logger.info("빌링키 발급 요청: customer_uid=${request.customer_uid}")
            
            val response = webClient.post()
                .uri("/subscribe/customers/${request.customer_uid}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .bodyValue(request)
                .retrieve()
                .awaitBody<BillingKeyResponse>()

            logger.info("빌링키 발급 응답: code=${response.code}, message=${response.message}")
            return response
            
        } catch (e: Exception) {
            logger.error("빌링키 발급 실패: customer_uid=${request.customer_uid}", e)
            throw RuntimeException("빌링키 발급 실패: ${e.message}", e)
        }
    }

    /**
     * 빌링키 정보 조회
     */
    suspend fun getBillingKeyInfo(customerUid: String): BillingKeyInfoResponse {
        val token = getAccessToken()
        
        try {
            val response = webClient.get()
                .uri("/subscribe/customers/$customerUid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .awaitBody<BillingKeyInfoResponse>()

            return response
            
        } catch (e: Exception) {
            logger.error("빌링키 정보 조회 실패: customer_uid=$customerUid", e)
            throw RuntimeException("빌링키 정보 조회 실패: ${e.message}", e)
        }
    }

    /**
     * 빌링키 삭제
     */
    suspend fun deleteBillingKey(customerUid: String): BillingKeyDeleteResponse {
        val token = getAccessToken()
        
        try {
            logger.info("빌링키 삭제 요청: customer_uid=$customerUid")
            
            val response = webClient.delete()
                .uri("/subscribe/customers/$customerUid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .awaitBody<BillingKeyDeleteResponse>()

            logger.info("빌링키 삭제 응답: code=${response.code}, message=${response.message}")
            return response
            
        } catch (e: Exception) {
            logger.error("빌링키 삭제 실패: customer_uid=$customerUid", e)
            throw RuntimeException("빌링키 삭제 실패: ${e.message}", e)
        }
    }

    /**
     * 구독 결제 요청
     */
    suspend fun requestSubscriptionPayment(request: SubscriptionPaymentRequest): SubscriptionPaymentResponse {
        val token = getAccessToken()
        
        try {
            logger.info("구독 결제 요청: merchant_uid=${request.merchant_uid}, amount=${request.amount}")
            
            val response = webClient.post()
                .uri("/subscribe/payments/again")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .bodyValue(request)
                .retrieve()
                .awaitBody<SubscriptionPaymentResponse>()

            logger.info("구독 결제 응답: code=${response.code}, status=${response.response?.status}")
            return response
            
        } catch (e: Exception) {
            logger.error("구독 결제 실패: merchant_uid=${request.merchant_uid}", e)
            throw RuntimeException("구독 결제 실패: ${e.message}", e)
        }
    }

    /**
     * 결제 정보 조회
     */
    suspend fun getPaymentInfo(impUid: String): PaymentInfoResponse {
        val token = getAccessToken()
        
        try {
            val response = webClient.get()
                .uri("/payments/$impUid")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .awaitBody<PaymentInfoResponse>()

            return response
            
        } catch (e: Exception) {
            logger.error("결제 정보 조회 실패: imp_uid=$impUid", e)
            throw RuntimeException("결제 정보 조회 실패: ${e.message}", e)
        }
    }

    /**
     * 결제 취소
     */
    suspend fun cancelPayment(request: PaymentCancelRequest): PaymentCancelResponse {
        val token = getAccessToken()
        
        try {
            logger.info("결제 취소 요청: imp_uid=${request.imp_uid}, amount=${request.amount}")
            
            val response = webClient.post()
                .uri("/payments/cancel")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .bodyValue(request)
                .retrieve()
                .awaitBody<PaymentCancelResponse>()

            logger.info("결제 취소 응답: code=${response.code}, status=${response.response?.status}")
            return response
            
        } catch (e: Exception) {
            logger.error("결제 취소 실패: imp_uid=${request.imp_uid}", e)
            throw RuntimeException("결제 취소 실패: ${e.message}", e)
        }
    }

    /**
     * 웹훅 서명 검증
     */
    fun verifyWebhookSignature(body: String, signature: String): Boolean {
        // PortOne 웹훅 서명 검증 로직
        // TODO: 실제 서명 검증 로직 구현
        return true
    }
}