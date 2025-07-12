package com.comdeply.comment.config

import com.comdeply.comment.app.web.svc.SubscriptionService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration
@EnableScheduling
class SchedulingConfig(
    private val subscriptionService: SubscriptionService
) {
    private val logger = LoggerFactory.getLogger(SchedulingConfig::class.java)

    /**
     * 매일 오전 2시에 구독 갱신 처리
     */
    @Scheduled(cron = "0 0 2 * * ?")
    fun processSubscriptionRenewals() {
        logger.info("구독 갱신 스케줄러 시작")
        try {
            subscriptionService.processSubscriptionRenewals()
            logger.info("구독 갱신 스케줄러 완료")
        } catch (e: Exception) {
            logger.error("구독 갱신 스케줄러 실행 중 오류 발생", e)
        }
    }

    /**
     * 매일 오전 3시에 만료된 구독 처리
     */
    @Scheduled(cron = "0 0 3 * * ?")
    fun processExpiredSubscriptions() {
        logger.info("만료 구독 처리 스케줄러 시작")
        try {
            subscriptionService.processExpiredSubscriptions()
            logger.info("만료 구독 처리 스케줄러 완료")
        } catch (e: Exception) {
            logger.error("만료 구독 처리 스케줄러 실행 중 오류 발생", e)
        }
    }
}
