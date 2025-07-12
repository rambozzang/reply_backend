package com.comdeply.comment.portone.service

import com.comdeply.comment.portone.dto.*
import com.comdeply.comment.portone.entity.BillingKey
import com.comdeply.comment.portone.entity.BillingKeyStatus
import com.comdeply.comment.portone.repository.BillingKeyRepository
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class PortOneBillingService(
    private val portOneApiClient: PortOneApiClient,
    private val billingKeyRepository: BillingKeyRepository
) {
    
    private val logger = LoggerFactory.getLogger(PortOneBillingService::class.java)

    /**
     * 빌링키 발급 및 저장
     */
    fun issueBillingKey(
        adminId: Long,
        cardNumber: String,
        expiry: String,
        birth: String,
        pwd2digit: String
    ): BillingKey = runBlocking {
        
        val customerUid = generateCustomerUid(adminId)
        
        // 기존 활성 빌링키가 있으면 삭제
        val existingBillingKey = billingKeyRepository.findByAdminIdAndStatus(adminId, BillingKeyStatus.ACTIVE)
        if (existingBillingKey != null) {
            deleteBillingKey(adminId)
        }

        try {
            val request = BillingKeyRequest(
                customer_uid = customerUid,
                card_number = cardNumber,
                expiry = expiry,
                birth = birth,
                pwd_2digit = pwd2digit
            )

            val response = portOneApiClient.issueBillingKey(request)
            
            if (response.code == 0 && response.response != null) {
                val billingKeyData = response.response
                
                // 로컬 DB에 저장
                val billingKey = BillingKey(
                    adminId = adminId,
                    billingKey = customerUid,
                    customerId = customerUid,
                    cardName = billingKeyData.card_name,
                    cardNumber = billingKeyData.card_number,
                    cardType = billingKeyData.card_type,
                    bank = billingKeyData.bank,
                    status = BillingKeyStatus.ACTIVE
                )

                return@runBlocking billingKeyRepository.save(billingKey)
            } else {
                throw RuntimeException("빌링키 발급 실패: ${response.message}")
            }
            
        } catch (e: Exception) {
            logger.error("빌링키 발급 실패: adminId=$adminId", e)
            throw RuntimeException("빌링키 발급에 실패했습니다: ${e.message}", e)
        }
    }

    /**
     * 빌링키 조회
     */
    fun getBillingKey(adminId: Long): BillingKey? {
        return billingKeyRepository.findByAdminIdAndStatus(adminId, BillingKeyStatus.ACTIVE)
    }

    /**
     * 빌링키 정보 조회 (PortOne에서 최신 정보)
     */
    fun getBillingKeyInfo(adminId: Long): BillingKeyData? = runBlocking {
        val billingKey = getBillingKey(adminId) ?: return@runBlocking null
        
        try {
            val response = portOneApiClient.getBillingKeyInfo(billingKey.customerId)
            
            if (response.code == 0) {
                return@runBlocking response.response
            } else {
                logger.warn("빌링키 정보 조회 실패: adminId=$adminId, message=${response.message}")
                return@runBlocking null
            }
            
        } catch (e: Exception) {
            logger.error("빌링키 정보 조회 실패: adminId=$adminId", e)
            return@runBlocking null
        }
    }

    /**
     * 빌링키 삭제
     */
    fun deleteBillingKey(adminId: Long): Boolean = runBlocking {
        val billingKey = getBillingKey(adminId) ?: return@runBlocking false
        
        try {
            val response = portOneApiClient.deleteBillingKey(billingKey.customerId)
            
            if (response.code == 0) {
                // 로컬 DB에서도 삭제 처리
                val deletedBillingKey = billingKey.copy(
                    status = BillingKeyStatus.DELETED,
                    deletedAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
                billingKeyRepository.save(deletedBillingKey)
                
                logger.info("빌링키 삭제 완료: adminId=$adminId")
                return@runBlocking true
            } else {
                logger.warn("빌링키 삭제 실패: adminId=$adminId, message=${response.message}")
                return@runBlocking false
            }
            
        } catch (e: Exception) {
            logger.error("빌링키 삭제 실패: adminId=$adminId", e)
            return@runBlocking false
        }
    }

    /**
     * 빌링키 유효성 확인
     */
    fun validateBillingKey(adminId: Long): Boolean {
        val billingKeyData = getBillingKeyInfo(adminId)
        return billingKeyData != null
    }

    /**
     * 고객 UID 생성
     */
    private fun generateCustomerUid(adminId: Long): String {
        return "admin_${adminId}_${System.currentTimeMillis()}"
    }

    /**
     * 빌링키 존재 여부 확인
     */
    fun hasBillingKey(adminId: Long): Boolean {
        return billingKeyRepository.existsByAdminIdAndStatus(adminId, BillingKeyStatus.ACTIVE)
    }

    /**
     * 관리자의 모든 빌링키 조회 (히스토리 포함)
     */
    fun getAllBillingKeys(adminId: Long): List<BillingKey> {
        return billingKeyRepository.findByAdminId(adminId)
    }
}