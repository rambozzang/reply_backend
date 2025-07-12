package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.AdminRole
import com.comdeply.comment.entity.AdminSite
import com.comdeply.comment.entity.SitePermission
import com.comdeply.comment.repository.AdminSiteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AdminPermissionService(
    private val adminSiteRepository: AdminSiteRepository
) {
    private val logger = LoggerFactory.getLogger(AdminPermissionService::class.java)

    /**
     * 관리자가 특정 사이트에 대한 권한이 있는지 확인
     */
    fun hasPermissionForSite(admin: Admin, siteId: Long, requiredPermission: SitePermission = SitePermission.READ_ONLY): Boolean {
        // SUPER_ADMIN은 모든 사이트에 대한 모든 권한을 가짐
        if (admin.role == AdminRole.SUPER_ADMIN) {
            logger.debug("SUPER_ADMIN {} has full access to site {}", admin.id, siteId)
            return true
        }

        // ADMIN은 할당된 사이트만 관리 가능
        if (admin.role == AdminRole.ADMIN) {
            val adminSite = adminSiteRepository.findByAdminIdAndSiteIdAndIsActiveTrue(admin.id, siteId)
            if (adminSite != null) {
                val hasPermission = when (requiredPermission) {
                    SitePermission.READ_ONLY -> true // 모든 권한은 읽기 권한을 포함
                    SitePermission.MODERATE -> adminSite.permission in listOf(SitePermission.MODERATE, SitePermission.MANAGE)
                    SitePermission.MANAGE -> adminSite.permission == SitePermission.MANAGE
                }
                logger.debug(
                    "ADMIN {} permission for site {}: {} (required: {})",
                    admin.id,
                    siteId,
                    hasPermission,
                    requiredPermission
                )
                return hasPermission
            }
            logger.debug("ADMIN {} has no permission for site {}", admin.id, siteId)
            return false
        }

        // MODERATOR는 권한이 없음 (특별히 할당된 경우만 제외)
        logger.debug("MODERATOR {} has no permission for site {}", admin.id, siteId)
        return false
    }

    /**
     * 관리자가 접근 가능한 사이트 ID 목록 조회
     */
    fun getAccessibleSiteIds(admin: Admin): List<Long> {
        return when (admin.role) {
            AdminRole.SUPER_ADMIN -> {
                // SUPER_ADMIN은 모든 사이트에 접근 가능 (빈 리스트 반환 시 전체 조회로 처리)
                emptyList()
            }
            AdminRole.ADMIN -> {
                adminSiteRepository.findSiteIdsByAdminId(admin.id)
            }
            AdminRole.MODERATOR -> {
                // MODERATOR는 특별히 할당된 사이트만 접근 가능
                adminSiteRepository.findSiteIdsByAdminId(admin.id)
            }
        }
    }

    /**
     * 관리자가 다른 관리자를 관리할 수 있는지 확인
     */
    fun canManageAdmin(currentAdmin: Admin, targetAdmin: Admin): Boolean {
        // SUPER_ADMIN만 다른 관리자를 관리할 수 있음
        if (currentAdmin.role != AdminRole.SUPER_ADMIN) {
            return false
        }

        // 자기 자신은 비활성화할 수 없음
        if (currentAdmin.id == targetAdmin.id) {
            return false
        }

        return true
    }

    /**
     * 관리자가 사이트 관리자를 할당할 수 있는지 확인
     */
    fun canAssignSiteAdmin(currentAdmin: Admin): Boolean {
        return currentAdmin.role == AdminRole.SUPER_ADMIN
    }

    /**
     * 관리자가 전체 통계를 볼 수 있는지 확인
     */
    fun canViewGlobalStats(admin: Admin): Boolean {
        return admin.role == AdminRole.SUPER_ADMIN
    }

    /**
     * 사이트에 관리자 할당
     */
    fun assignAdminToSite(
        assignerId: Long,
        adminId: Long,
        siteId: Long,
        permission: SitePermission = SitePermission.MANAGE
    ): AdminSite {
        // 중복 확인
        if (adminSiteRepository.existsByAdminIdAndSiteIdAndIsActiveTrue(adminId, siteId)) {
            throw IllegalArgumentException("이미 해당 사이트에 할당된 관리자입니다")
        }

        val adminSite = AdminSite(
            adminId = adminId,
            siteId = siteId,
            permission = permission,
            assignedBy = assignerId
        )

        return adminSiteRepository.save(adminSite)
    }

    /**
     * 사이트에서 관리자 할당 해제
     */
    fun unassignAdminFromSite(adminId: Long, siteId: Long): Boolean {
        val adminSite = adminSiteRepository.findByAdminIdAndSiteIdAndIsActiveTrue(adminId, siteId)
        return if (adminSite != null) {
            val updatedAdminSite = adminSite.copy(
                isActive = false,
                updatedAt = LocalDateTime.now()
            )
            adminSiteRepository.save(updatedAdminSite)
            true
        } else {
            false
        }
    }

    /**
     * 관리자의 사이트별 권한 조회
     */
    fun getAdminSitePermissions(adminId: Long): List<AdminSite> {
        return adminSiteRepository.findByAdminIdAndIsActiveTrue(adminId)
    }

    /**
     * 사이트의 관리자 목록 조회
     */
    fun getSiteAdmins(siteId: Long): List<AdminSite> {
        return adminSiteRepository.findBySiteIdAndIsActiveTrue(siteId)
    }
}
