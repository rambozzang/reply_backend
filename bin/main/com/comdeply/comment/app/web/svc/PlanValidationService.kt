package com.comdeply.comment.app.web.svc

import com.comdeply.comment.config.PlanLimits
import com.comdeply.comment.dto.common.ValidationResult
import com.comdeply.comment.entity.SubscriptionStatus
import com.comdeply.comment.repository.CommentRepository
import com.comdeply.comment.repository.SiteRepository
import com.comdeply.comment.repository.SiteThemeRepository
import com.comdeply.comment.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class PlanValidationService(
    private val subscriptionRepository: SubscriptionRepository,
    private val siteRepository: SiteRepository,
    private val commentRepository: CommentRepository,
    private val siteThemeRepository: SiteThemeRepository
) {

    /**
     * 관리자의 현재 플랜 조회
     */
    fun getAdminPlan(adminId: Long): PlanLimits.Plan {
        val subscription = subscriptionRepository.findByAdminIdAndStatus(adminId, SubscriptionStatus.ACTIVE)
            ?: return PlanLimits.Plan.STARTER // 기본 플랜

        return when (subscription.planName.uppercase()) {
            "STARTER" -> PlanLimits.Plan.STARTER
            "PRO" -> PlanLimits.Plan.PRO
            "ENTERPRISE" -> PlanLimits.Plan.ENTERPRISE
            else -> PlanLimits.Plan.STARTER
        }
    }

    /**
     * 사용자의 현재 플랜 조회 (기존 호환성 유지)
     */
    fun getUserPlan(userId: Long): PlanLimits.Plan {
        // User는 댓글 작성자이므로 기본 플랜 반환
        return PlanLimits.Plan.STARTER
    }

    /**
     * 도메인 추가 가능 여부 확인 (관리자용)
     */
    fun canAddDomains(adminId: Long, currentDomainCount: Int, additionalDomains: Int): ValidationResult {
        val plan = getAdminPlan(adminId)
        val limit = PlanLimits.getDomainLimit(plan)
        val totalDomains = currentDomainCount + additionalDomains

        if (totalDomains > limit) {
            return ValidationResult.error(
                "현재 플랜(${plan.name})에서는 최대 ${limit}개의 도메인만 등록할 수 있습니다. (현재: ${currentDomainCount}개, 추가 요청: ${additionalDomains}개)",
                "DOMAIN_LIMIT_EXCEEDED"
            )
        }

        return ValidationResult.success("도메인 추가 가능")
    }

    /**
     * 사이트 생성 가능 여부 확인 (관리자용)
     */
    fun canCreateSite(adminId: Long): ValidationResult {
        val plan = getAdminPlan(adminId)
        val currentSiteCount = siteRepository.countByOwnerIdAndIsActiveTrue(adminId)
        val limit = PlanLimits.getSiteLimit(plan)

        if (currentSiteCount >= limit) {
            return ValidationResult.error(
                "현재 플랜(${plan.name})에서는 최대 ${limit}개의 사이트만 생성할 수 있습니다. (현재: ${currentSiteCount}개)",
                "SITE_LIMIT_EXCEEDED"
            )
        }

        return ValidationResult.success("사이트 생성 가능")
    }

    /**
     * 월간 댓글 수 확인
     */
    fun canCreateComment(siteId: Long, userId: Long): ValidationResult {
        val plan = getUserPlan(userId)
        val limit = PlanLimits.getCommentLimit(plan)

        // 현재 월의 댓글 수 조회
        val currentMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
        val nextMonth = currentMonth.plusMonths(1)

        val currentMonthComments = commentRepository.countByOwnerIdAndCreatedAtBetween(
            userId,
            currentMonth,
            nextMonth
        )

        if (currentMonthComments >= limit) {
            return ValidationResult.error(
                "현재 플랜(${plan.name})의 월간 댓글 한도(${limit}개)를 초과했습니다. (현재: ${currentMonthComments}개)",
                "COMMENT_LIMIT_EXCEEDED"
            )
        }

        return ValidationResult.success("댓글 생성 가능")
    }

    /**
     * 테마 적용 가능 여부 확인
     */
    fun canApplyTheme(userId: Long, siteId: Long): ValidationResult {
        val plan = getUserPlan(userId)
        val limit = PlanLimits.getPageThemeLimit(plan)

        // 현재 사이트의 활성 테마 수 조회
        val currentThemeCount = siteThemeRepository.countBySiteIdAndIsActiveTrue(siteId)

        if (currentThemeCount >= limit) {
            return ValidationResult.error(
                "현재 플랜(${plan.name})에서는 사이트당 최대 ${limit}개의 테마만 적용할 수 있습니다. (현재: ${currentThemeCount}개)",
                "THEME_LIMIT_EXCEEDED"
            )
        }

        return ValidationResult.success("테마 적용 가능")
    }

    /**
     * 파일 업로드 크기 확인
     */
    fun canUploadFile(userId: Long, fileSizeMB: Long): ValidationResult {
        val plan = getUserPlan(userId)
        val limit = PlanLimits.getFileUploadLimit(plan)

        if (fileSizeMB > limit) {
            return ValidationResult.error(
                "현재 플랜(${plan.name})에서는 최대 ${limit}MB까지 업로드할 수 있습니다. (요청: ${fileSizeMB}MB)",
                "FILE_SIZE_LIMIT_EXCEEDED"
            )
        }

        return ValidationResult.success("파일 업로드 가능")
    }

    /**
     * 사용자 플랜 정보 조회
     */
    fun getUserPlanInfo(userId: Long): PlanInfo {
        val plan = getUserPlan(userId)

        return PlanInfo(
            plan = plan.name,
            limits = PlanLimitsInfo(
                domains = PlanLimits.getDomainLimit(plan),
                sites = PlanLimits.getSiteLimit(plan),
                monthlyComments = PlanLimits.getCommentLimit(plan),
                themes = PlanLimits.getThemeLimit(plan),
                pageThemes = PlanLimits.getPageThemeLimit(plan),
                fileUploadMB = PlanLimits.getFileUploadLimit(plan),
                admins = PlanLimits.getAdminLimit(plan)
            ),
            usage = getCurrentUsage(userId)
        )
    }

    private fun getCurrentUsage(userId: Long): PlanUsageInfo {
        val currentSites = siteRepository.countByOwnerIdAndIsActiveTrue(userId)

        // 현재 월의 댓글 수
        val currentMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
        val nextMonth = currentMonth.plusMonths(1)
        val currentMonthComments = commentRepository.countByOwnerIdAndCreatedAtBetween(
            userId,
            currentMonth,
            nextMonth
        )

        return PlanUsageInfo(
            sites = currentSites.toInt(),
            monthlyComments = currentMonthComments,
            // TODO: 도메인 수, 테마 수 등 추가 사용량 조회
            domains = 0,
            themes = 0
        )
    }

    data class PlanInfo(
        val plan: String,
        val limits: PlanLimitsInfo,
        val usage: PlanUsageInfo
    )

    data class PlanLimitsInfo(
        val domains: Int,
        val sites: Int,
        val monthlyComments: Long,
        val themes: Int,
        val pageThemes: Int,
        val fileUploadMB: Long,
        val admins: Int
    )

    data class PlanUsageInfo(
        val sites: Int,
        val monthlyComments: Long,
        val domains: Int,
        val themes: Int
    )
}
