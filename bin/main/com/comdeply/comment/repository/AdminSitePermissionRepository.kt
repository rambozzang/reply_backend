package com.comdeply.comment.repository

import com.comdeply.comment.entity.AdminSitePermission
import com.comdeply.comment.entity.SitePermissionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminSitePermissionRepository : JpaRepository<AdminSitePermission, Long> {
    fun findByAdminIdAndIsActiveTrue(adminId: Long): List<AdminSitePermission>
    fun findByAdminIdAndSiteIdAndIsActiveTrue(adminId: Long, siteId: String): AdminSitePermission?
    fun findBySiteIdAndIsActiveTrue(siteId: String): List<AdminSitePermission>
    fun findByAdminIdAndPermissionAndIsActiveTrue(adminId: Long, permission: SitePermissionType): List<AdminSitePermission>
    fun existsByAdminIdAndSiteIdAndIsActiveTrue(adminId: Long, siteId: String): Boolean
}