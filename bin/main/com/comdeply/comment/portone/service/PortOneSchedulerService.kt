package com.comdeply.comment.portone.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class PortOneSchedulerService(
    private val portOneSubscriptionService: PortOneSubscriptionService
) {
    
    private val logger = LoggerFactory.getLogger(PortOneSchedulerService::class.java)

    /**
     * 정기 결제 처리 (매일 오전 9시 실행)
     */
    @Scheduled(cron = "0 0 9 * * *")
    fun processScheduledPayments() {
        logger.info("정기 결제 배치 작업 시작")
        
        try {
            portOneSubscriptionService.processScheduledPayments()
            logger.info("정기 결제 배치 작업 완료")
        } catch (e: Exception) {
            logger.error("정기 결제 배치 작업 실패", e)
        }
    }

    /**
     * 수동 정기 결제 실행 (테스트용)
     */
    fun triggerScheduledPayments() {
        logger.info("수동 정기 결제 실행")
        processScheduledPayments()
    }
}