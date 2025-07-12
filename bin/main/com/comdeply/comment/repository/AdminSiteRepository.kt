package com.comdeply.comment.repository

import com.comdeply.comment.entity.AdminSite
import com.comdeply.comment.entity.SitePermission
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AdminSiteRepository : JpaRepository<AdminSite, Long> {

    // 관리자가 관리하는 사이트 목록
    fun findByAdminIdAndIsActiveTrue(adminId: Long): List<AdminSite>

    // 사이트를 관리하는 관리자 목록
    fun findBySiteIdAndIsActiveTrue(siteId: Long): List<AdminSite>

    // 특정 관리자가 특정 사이트에 대한 권한이 있는지 확인
    fun findByAdminIdAndSiteIdAndIsActiveTrue(adminId: Long, siteId: Long): AdminSite?

    // 관리자의 사이트별 권한 조회
    @Query("SELECT a FROM AdminSite a WHERE a.adminId = :adminId AND a.siteId = :siteId AND a.isActive = true")
    fun findAdminSitePermission(@Param("adminId") adminId: Long, @Param("siteId") siteId: Long): AdminSite?

    // 특정 권한을 가진 관리자 목록
    fun findByPermissionAndIsActiveTrue(permission: SitePermission): List<AdminSite>

    // 관리자가 관리하는 사이트 ID 목록만 조회
    @Query("SELECT a.siteId FROM AdminSite a WHERE a.adminId = :adminId AND a.isActive = true")
    fun findSiteIdsByAdminId(@Param("adminId") adminId: Long): List<Long>

    // 사이트에 할당된 관리자 ID 목록만 조회
    @Query("SELECT a.adminId FROM AdminSite a WHERE a.siteId = :siteId AND a.isActive = true")
    fun findAdminIdsBySiteId(@Param("siteId") siteId: Long): List<Long>

    // 관리자별 관리 사이트 수
    @Query("SELECT COUNT(a) FROM AdminSite a WHERE a.adminId = :adminId AND a.isActive = true")
    fun countManagedSitesByAdminId(@Param("adminId") adminId: Long): Long

    // 사이트별 관리자 수
    @Query("SELECT COUNT(a) FROM AdminSite a WHERE a.siteId = :siteId AND a.isActive = true")
    fun countAdminsBySiteId(@Param("siteId") siteId: Long): Long

    // 페이지네이션으로 관리자의 사이트 조회
    fun findByAdminIdAndIsActiveTrue(adminId: Long, pageable: Pageable): Page<AdminSite>

    // 페이지네이션으로 사이트의 관리자 조회
    fun findBySiteIdAndIsActiveTrue(siteId: Long, pageable: Pageable): Page<AdminSite>

    // 중복 할당 방지를 위한 존재 확인
    fun existsByAdminIdAndSiteIdAndIsActiveTrue(adminId: Long, siteId: Long): Boolean
}
