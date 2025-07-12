package com.comdeply.comment.utils

data class PlanLimits(
    val monthlyCommentLimit: Int,
    val maxSites: Int,
    val maxUsers: Int,
    val themeLimit: Int,
    val supportLevel: String
) {
    companion object {
        fun getByPlan(planType: PlanType): PlanLimits {
            return when (planType) {
                PlanType.FREE, PlanType.STARTER -> PlanLimits(
                    monthlyCommentLimit = 1000,
                    maxSites = 1,
                    maxUsers = 100,
                    themeLimit = 3,
                    supportLevel = "Community"
                )
                PlanType.PRO -> PlanLimits(
                    monthlyCommentLimit = 10000,
                    maxSites = 5,
                    maxUsers = 1000,
                    themeLimit = 10,
                    supportLevel = "Email"
                )
                PlanType.PREMIUM -> PlanLimits(
                    monthlyCommentLimit = 50000,
                    maxSites = 10,
                    maxUsers = 5000,
                    themeLimit = 20,
                    supportLevel = "Priority"
                )
                PlanType.ENTERPRISE -> PlanLimits(
                    monthlyCommentLimit = -1, // 무제한
                    maxSites = -1, // 무제한
                    maxUsers = -1, // 무제한
                    themeLimit = -1, // 무제한
                    supportLevel = "24/7 Dedicated"
                )
            }
        }

        // 하위 호환성을 위한 메서드들
        fun getCommentLimit(planType: PlanType): Long {
            return getByPlan(planType).monthlyCommentLimit.toLong()
        }

        fun getSiteLimit(planType: PlanType): Long {
            return getByPlan(planType).maxSites.toLong()
        }
    }
}
