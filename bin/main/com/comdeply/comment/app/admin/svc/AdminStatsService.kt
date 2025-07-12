package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.app.admin.svc.vo.AdvancedStatsResponse
import com.comdeply.comment.app.admin.svc.vo.DetailedStatsResponse
import com.comdeply.comment.app.admin.svc.vo.PerformanceStatsResponse
import com.comdeply.comment.app.admin.svc.vo.SiteStatsItem
import com.comdeply.comment.app.admin.svc.vo.StatsResponse
import com.comdeply.comment.app.admin.svc.vo.TrendStatsResponse
import com.comdeply.comment.app.admin.svc.vo.WeeklyCommentsItem
import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.PaymentStatus
import com.comdeply.comment.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class AdminStatsService(
    private val adminPermissionService: AdminPermissionService,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val siteRepository: SiteRepository,
    private val ssoUserRepository: SsoUserRepository,
    private val ssoSessionRepository: SsoSessionRepository,
    private val paymentRepository: PaymentRepository
) {
    private val logger = LoggerFactory.getLogger(AdminStatsService::class.java)

    /**
     * 상세 통계 조회
     */
    @Transactional(readOnly = true)
    fun getDetailedStats(admin: Admin): DetailedStatsResponse {
        logger.info("상세 통계 조회: adminId={}", admin.id)

        try {
            val now = LocalDateTime.now()
            val startOfDay = now.toLocalDate().atStartOfDay()

            // 권한에 따른 사이트 목록 가져오기
            val accessibleSiteIds = adminPermissionService.getAccessibleSiteIds(admin)
            val isGlobalAccess = adminPermissionService.canViewGlobalStats(admin)

            val summary: StatsResponse
            val siteStats: List<SiteStatsItem>
            val weeklyComments: List<WeeklyCommentsItem>

            if (isGlobalAccess) {
                summary = getGlobalStats(now, startOfDay)
                siteStats = getGlobalSiteStats()
                weeklyComments = getGlobalWeeklyComments(now)
            } else {
                summary = getSiteBasedStats(accessibleSiteIds, now, startOfDay)
                siteStats = getSiteBasedSiteStats(accessibleSiteIds)
                weeklyComments = getSiteBasedWeeklyComments(accessibleSiteIds, now)
            }

            val response = DetailedStatsResponse(
                summary = summary,
                siteStats = siteStats,
                weeklyComments = weeklyComments,
                lastUpdated = now
            )

            logger.info("상세 통계 조회 완료: adminId={}, totalComments={}", admin.id, summary.totalComments)
            return response
        } catch (e: Exception) {
            logger.error("상세 통계 조회 중 오류 발생: adminId={}", admin.id, e)
            throw e
        }
    }

    /**
     * 고급 통계 조회
     */
    @Transactional(readOnly = true)
    fun getAdvancedStats(admin: Admin): AdvancedStatsResponse {
        logger.info("고급 통계 조회: adminId={}", admin.id)

        try {
            // TODO: 고급 통계 로직 구현
            val response = AdvancedStatsResponse(
                message = "고급 통계 기능은 개발 중입니다",
                features = listOf(
                    "사용자 행동 분석",
                    "댓글 감정 분석",
                    "트래픽 패턴 분석",
                    "수익 예측"
                ),
                lastUpdated = LocalDateTime.now()
            )

            logger.info("고급 통계 조회 완료: adminId={}", admin.id)
            return response
        } catch (e: Exception) {
            logger.error("고급 통계 조회 중 오류 발생: adminId={}", admin.id, e)
            throw e
        }
    }

    /**
     * 성능 통계 조회
     */
    @Transactional(readOnly = true)
    fun getPerformanceStats(admin: Admin): PerformanceStatsResponse {
        logger.info("성능 통계 조회: adminId={}", admin.id)

        try {
            // TODO: 성능 통계 로직 구현
            val response = PerformanceStatsResponse(
                message = "성능 통계 기능은 개발 중입니다",
                metrics = listOf(
                    "응답 시간",
                    "처리량",
                    "오류율",
                    "리소스 사용량"
                ),
                lastUpdated = LocalDateTime.now()
            )

            logger.info("성능 통계 조회 완료: adminId={}", admin.id)
            return response
        } catch (e: Exception) {
            logger.error("성능 통계 조회 중 오류 발생: adminId={}", admin.id, e)
            throw e
        }
    }

    /**
     * 트렌드 통계 조회
     */
    @Transactional(readOnly = true)
    fun getTrendStats(admin: Admin): TrendStatsResponse {
        logger.info("트렌드 통계 조회: adminId={}", admin.id)

        try {
            // TODO: 트렌드 통계 로직 구현
            val response = TrendStatsResponse(
                message = "트렌드 통계 기능은 개발 중입니다",
                trends = listOf(
                    "댓글 증가율",
                    "사용자 참여율",
                    "인기 페이지",
                    "활성 시간대"
                ),
                lastUpdated = LocalDateTime.now()
            )

            logger.info("트렌드 통계 조회 완료: adminId={}", admin.id)
            return response
        } catch (e: Exception) {
            logger.error("트렌드 통계 조회 중 오류 발생: adminId={}", admin.id, e)
            throw e
        }
    }

    /**
     * 전역 통계 조회 (수퍼관리자용)
     */
    private fun getGlobalStats(now: LocalDateTime, startOfDay: LocalDateTime): StatsResponse {
        val totalComments = commentRepository.count()
        val totalUsers = userRepository.count()
        val totalSites = siteRepository.count()
        val todayComments = commentRepository.countByCreatedAtAfter(startOfDay)

        // SSO 통계
        val ssoUsers = ssoUserRepository.countByIsActiveTrue()
        val activeSsoSessions = ssoSessionRepository.countByIsActiveTrueAndExpiresAtAfter(now)
        val todaySsoLogins = ssoSessionRepository.countByCreatedAtAfter(startOfDay)
        val ssoActiveSites = siteRepository.countBySsoEnabledTrue()

        // 결제 통계
        val totalPayments = paymentRepository.count()
        val totalRevenue = paymentRepository.getTotalRevenue()
        val paidPayments = paymentRepository.countByStatus(PaymentStatus.PAID)

        return StatsResponse(
            totalComments = totalComments,
            totalUsers = totalUsers,
            totalSites = totalSites,
            todayComments = todayComments,
            ssoUsers = ssoUsers,
            activeSsoSessions = activeSsoSessions,
            todaySsoLogins = todaySsoLogins,
            ssoActiveSites = ssoActiveSites,
            totalPayments = totalPayments,
            totalRevenue = totalRevenue,
            paidPayments = paidPayments
        )
    }

    /**
     * 사이트 기반 통계 조회 (일반 관리자용)
     */
    private fun getSiteBasedStats(accessibleSiteIds: List<Long>, now: LocalDateTime, startOfDay: LocalDateTime): StatsResponse {
        val totalComments = if (accessibleSiteIds.isNotEmpty()) {
            commentRepository.countBySiteIdIn(accessibleSiteIds)
        } else {
            0L
        }

        val totalUsers = if (accessibleSiteIds.isNotEmpty()) {
            userRepository.countBySiteIdIn(accessibleSiteIds)
        } else {
            0L
        }

        val totalSites = accessibleSiteIds.size.toLong()

        val todayComments = if (accessibleSiteIds.isNotEmpty()) {
            commentRepository.countBySiteIdInAndCreatedAtAfter(accessibleSiteIds, startOfDay)
        } else {
            0L
        }

        // SSO 통계
        val ssoUsers = if (accessibleSiteIds.isNotEmpty()) {
            ssoUserRepository.countBySiteIdInAndIsActiveTrue(accessibleSiteIds)
        } else {
            0L
        }

        val activeSsoSessions = if (accessibleSiteIds.isNotEmpty()) {
            ssoSessionRepository.countBySiteIdInAndIsActiveTrueAndExpiresAtAfter(accessibleSiteIds, now)
        } else {
            0L
        }

        val todaySsoLogins = if (accessibleSiteIds.isNotEmpty()) {
            ssoSessionRepository.countBySiteIdInAndCreatedAtAfter(accessibleSiteIds, startOfDay)
        } else {
            0L
        }

        val ssoActiveSites = if (accessibleSiteIds.isNotEmpty()) {
            siteRepository.countByIdInAndSsoEnabledTrue(accessibleSiteIds)
        } else {
            0L
        }

        // 결제 통계
        val siteAdminIds = if (accessibleSiteIds.isNotEmpty()) {
            siteRepository.findAllById(accessibleSiteIds).map { it.ownerId }
        } else {
            emptyList()
        }

        val totalPayments = if (siteAdminIds.isNotEmpty()) {
            paymentRepository.countByAdminIdIn(siteAdminIds)
        } else {
            0L
        }

        val totalRevenue = if (siteAdminIds.isNotEmpty()) {
            paymentRepository.getTotalRevenueByAdminIds(siteAdminIds)
        } else {
            0L
        }

        val paidPayments = if (siteAdminIds.isNotEmpty()) {
            paymentRepository.countByAdminIdInAndStatus(siteAdminIds, PaymentStatus.PAID)
        } else {
            0L
        }

        return StatsResponse(
            totalComments = totalComments,
            totalUsers = totalUsers,
            totalSites = totalSites,
            todayComments = todayComments,
            ssoUsers = ssoUsers,
            activeSsoSessions = activeSsoSessions,
            todaySsoLogins = todaySsoLogins,
            ssoActiveSites = ssoActiveSites,
            totalPayments = totalPayments,
            totalRevenue = totalRevenue,
            paidPayments = paidPayments
        )
    }

    /**
     * 전역 사이트 통계 조회
     */
    private fun getGlobalSiteStats(): List<SiteStatsItem> {
        return siteRepository.findAll().map { site ->
            val commentCount = commentRepository.countBySiteId(site.id)
            SiteStatsItem(
                id = site.id,
                name = site.siteName,
                domain = site.domain,
                commentCount = commentCount
            )
        }.filter { it.commentCount > 0 }
    }

    /**
     * 사이트 기반 사이트 통계 조회
     */
    private fun getSiteBasedSiteStats(accessibleSiteIds: List<Long>): List<SiteStatsItem> {
        return if (accessibleSiteIds.isNotEmpty()) {
            siteRepository.findAllById(accessibleSiteIds).map { site ->
                val commentCount = commentRepository.countBySiteId(site.id)
                SiteStatsItem(
                    id = site.id,
                    name = site.siteName,
                    domain = site.domain,
                    commentCount = commentCount
                )
            }.filter { it.commentCount > 0 }
        } else {
            emptyList()
        }
    }

    /**
     * 전역 주간 댓글 통계 조회
     */
    private fun getGlobalWeeklyComments(now: LocalDateTime): List<WeeklyCommentsItem> {
        return (0..6).map { daysAgo ->
            val date = now.minusDays(daysAgo.toLong()).toLocalDate()
            val startOfTargetDay = date.atStartOfDay()
            val endOfTargetDay = startOfTargetDay.plusDays(1)
            val count = commentRepository.countByCreatedAtBetween(startOfTargetDay, endOfTargetDay)

            WeeklyCommentsItem(
                date = date.toString(),
                count = count
            )
        }.reversed()
    }

    /**
     * 사이트 기반 주간 댓글 통계 조회
     */
    private fun getSiteBasedWeeklyComments(accessibleSiteIds: List<Long>, now: LocalDateTime): List<WeeklyCommentsItem> {
        return (0..6).map { daysAgo ->
            val date = now.minusDays(daysAgo.toLong()).toLocalDate()
            val startOfTargetDay = date.atStartOfDay()
            val endOfTargetDay = startOfTargetDay.plusDays(1)
            val count = if (accessibleSiteIds.isNotEmpty()) {
                commentRepository.countBySiteIdInAndCreatedAtBetween(accessibleSiteIds, startOfTargetDay, endOfTargetDay)
            } else {
                0L
            }

            WeeklyCommentsItem(
                date = date.toString(),
                count = count
            )
        }.reversed()
    }
}
