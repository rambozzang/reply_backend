package com.comdeply.comment.repository

import com.comdeply.comment.entity.SsoUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SsoUserRepository : JpaRepository<SsoUser, Long> {

    // 사이트별 외부 사용자 ID로 조회
    fun findBySiteIdAndExternalUserId(siteId: Long, externalUserId: String): SsoUser?

    // 사이트별 SSO 사용자 목록
    fun findBySiteIdAndIsActiveTrueOrderByLastLoginAtDesc(siteId: Long, pageable: Pageable): Page<SsoUser>

    // 사용자 ID로 SSO 연동 정보 조회
    fun findByUserIdAndIsActiveTrue(userId: Long): List<SsoUser>

    // 특정 기간 내 로그인한 사용자
    @Query(
        """
        SELECT su FROM SsoUser su 
        WHERE su.siteId = :siteId 
        AND su.isActive = true 
        AND su.lastLoginAt >= :since
        ORDER BY su.lastLoginAt DESC
    """
    )
    fun findRecentActiveUsers(
        @Param("siteId") siteId: Long,
        @Param("since") since: LocalDateTime
    ): List<SsoUser>

    // 사이트별 SSO 사용자 수 조회
    fun countBySiteIdAndIsActiveTrue(siteId: Long): Long

    // 오늘 로그인한 사용자 수
    @Query(
        """
        SELECT COUNT(su) FROM SsoUser su 
        WHERE su.siteId = :siteId 
        AND su.isActive = true 
        AND su.lastLoginAt >= :startOfDay
    """
    )
    fun countTodayLogins(@Param("siteId") siteId: Long, @Param("startOfDay") startOfDay: LocalDateTime): Long

    // 이번 주 로그인한 사용자 수
    @Query(
        """
        SELECT COUNT(su) FROM SsoUser su 
        WHERE su.siteId = :siteId 
        AND su.isActive = true 
        AND su.lastLoginAt >= :startOfWeek
    """
    )
    fun countWeeklyLogins(@Param("siteId") siteId: Long, @Param("startOfWeek") startOfWeek: LocalDateTime): Long

    // 이번 달 로그인한 사용자 수
    @Query(
        """
        SELECT COUNT(su) FROM SsoUser su 
        WHERE su.siteId = :siteId 
        AND su.isActive = true 
        AND su.lastLoginAt >= :startOfMonth
    """
    )
    fun countMonthlyLogins(@Param("siteId") siteId: Long, @Param("startOfMonth") startOfMonth: LocalDateTime): Long

    // 일별 로그인 통계 (최근 N일)
    @Query(
        """
        SELECT DATE(su.lastLoginAt) as loginDate, COUNT(su) as loginCount 
        FROM SsoUser su 
        WHERE su.siteId = :siteId 
        AND su.isActive = true 
        AND su.lastLoginAt >= :since
        GROUP BY DATE(su.lastLoginAt)
        ORDER BY loginDate DESC
    """
    )
    fun getDailyLoginStats(@Param("siteId") siteId: Long, @Param("since") since: LocalDateTime): List<Array<Any>>

    // 외부 사용자 ID 중복 확인
    fun existsBySiteIdAndExternalUserIdAndIdNot(siteId: Long, externalUserId: String, id: Long): Boolean

    // 사이트별 비활성 사용자 정리
    @Query(
        """
        SELECT su FROM SsoUser su 
        WHERE su.siteId = :siteId 
        AND su.isActive = true 
        AND su.lastLoginAt < :threshold
    """
    )
    fun findInactiveUsers(@Param("siteId") siteId: Long, @Param("threshold") threshold: LocalDateTime): List<SsoUser>

    // 관리자용 추가 쿼리
    fun findByLastLoginAtAfter(lastLoginAt: LocalDateTime): List<SsoUser>

    // 전체 활성 SSO 사용자 수 (SUPER_ADMIN용)
    fun countByIsActiveTrue(): Long

    // 여러 사이트의 활성 SSO 사용자 수 (일반 관리자용)
    fun countBySiteIdInAndIsActiveTrue(siteIds: List<Long>): Long
}
