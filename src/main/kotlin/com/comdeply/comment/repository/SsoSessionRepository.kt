package com.comdeply.comment.repository

import com.comdeply.comment.entity.SsoSession
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SsoSessionRepository : JpaRepository<SsoSession, Long> {

    // 토큰으로 세션 조회
    fun findBySessionTokenAndIsActiveTrue(sessionToken: String): SsoSession?

    // 사용자의 활성 세션 조회
    fun findBySsoUserIdAndIsActiveTrueOrderByCreatedAtDesc(ssoUserId: Long): List<SsoSession>

    // 만료된 세션 정리
    fun findByExpiresAtBeforeAndIsActiveTrue(expiresAt: LocalDateTime): List<SsoSession>

    // 세션 비활성화
    @Modifying
    @Query("UPDATE SsoSession s SET s.isActive = false WHERE s.id = :sessionId")
    fun deactivateSession(@Param("sessionId") sessionId: Long): Int

    // 사용자의 모든 세션 비활성화
    @Modifying
    @Query("UPDATE SsoSession s SET s.isActive = false WHERE s.ssoUserId = :ssoUserId")
    fun deactivateAllUserSessions(@Param("ssoUserId") ssoUserId: Long): Int

    // 만료된 세션 정리
    @Modifying
    @Query("UPDATE SsoSession s SET s.isActive = false WHERE s.expiresAt < :now")
    fun deactivateExpiredSessions(@Param("now") now: LocalDateTime): Int

    // 사용자별 활성 세션 수 조회
    fun countBySsoUserIdAndIsActiveTrue(ssoUserId: Long): Long

    // 사이트별 활성 세션 통계
    @Query(
        """
        SELECT COUNT(s) FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId = :siteId AND s.isActive = true
    """
    )
    fun countActiveSessions(@Param("siteId") siteId: Long): Long

    // 세션 활동 시간 업데이트
    @Modifying
    @Query("UPDATE SsoSession s SET s.lastActivityAt = :now WHERE s.id = :sessionId")
    fun updateLastActivity(@Param("sessionId") sessionId: Long, @Param("now") now: LocalDateTime): Int

    // 사이트별 평균 세션 지속 시간 (분)
    @Query(
        """
        SELECT AVG(TIMESTAMPDIFF(MINUTE, s.createdAt, COALESCE(s.lastActivityAt, s.createdAt))) 
        FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId = :siteId 
        AND s.createdAt >= :since
    """
    )
    fun getAvgSessionDurationMinutes(@Param("siteId") siteId: Long, @Param("since") since: LocalDateTime): Double?

    // 브라우저별 세션 통계
    @Query(
        """
        SELECT s.userAgent, COUNT(s) as sessionCount
        FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId = :siteId 
        AND s.createdAt >= :since
        AND s.userAgent IS NOT NULL
        GROUP BY s.userAgent
        ORDER BY sessionCount DESC
    """
    )
    fun getBrowserStats(@Param("siteId") siteId: Long, @Param("since") since: LocalDateTime): List<Array<Any>>

    // IP별 세션 수 (보안 모니터링)
    @Query(
        """
        SELECT s.ipAddress, COUNT(s) as sessionCount
        FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId = :siteId 
        AND s.createdAt >= :since
        AND s.ipAddress IS NOT NULL
        GROUP BY s.ipAddress
        HAVING COUNT(s) > :threshold
        ORDER BY sessionCount DESC
    """
    )
    fun findSuspiciousIpAddresses(
        @Param("siteId") siteId: Long,
        @Param("since") since: LocalDateTime,
        @Param("threshold") threshold: Long
    ): List<Array<Any>>

    // 동시 로그인 사용자 수
    @Query(
        """
        SELECT COUNT(DISTINCT s.ssoUserId) 
        FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId = :siteId 
        AND s.isActive = true 
        AND s.lastActivityAt >= :since
    """
    )
    fun countConcurrentUsers(@Param("siteId") siteId: Long, @Param("since") since: LocalDateTime): Long

    // 관리자용 추가 쿼리
    fun findByIsActiveTrueAndExpiresAtAfter(expiresAt: LocalDateTime): List<SsoSession>

    // 사이트별 세션 조회 (관리자용)
    @Query(
        """
        SELECT s FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId = :siteId AND s.isActive = true
        ORDER BY s.createdAt DESC
    """
    )
    fun findBySiteIdAndIsActiveTrueOrderByCreatedAtDesc(@Param("siteId") siteId: Long): List<SsoSession>

    // 페이지네이션 지원 사이트별 세션 조회
    @Query(
        """
        SELECT s FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId = :siteId AND s.isActive = true
        ORDER BY s.createdAt DESC
    """
    )
    fun findBySiteIdAndIsActiveTrueOrderByCreatedAtDescPaged(@Param("siteId") siteId: Long, pageable: Pageable): Page<SsoSession>

    // 오늘 로그인 수
    @Query(
        """
        SELECT COUNT(s) FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId = :siteId AND s.createdAt >= :startOfDay
    """
    )
    fun countTodayLogins(@Param("siteId") siteId: Long, @Param("startOfDay") startOfDay: LocalDateTime): Long

    // 이번 주 로그인 수
    @Query(
        """
        SELECT COUNT(s) FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId = :siteId AND s.createdAt >= :startOfWeek
    """
    )
    fun countWeeklyLogins(@Param("siteId") siteId: Long, @Param("startOfWeek") startOfWeek: LocalDateTime): Long

    // 이번 달 로그인 수
    @Query(
        """
        SELECT COUNT(s) FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId = :siteId AND s.createdAt >= :startOfMonth
    """
    )
    fun countMonthlyLogins(@Param("siteId") siteId: Long, @Param("startOfMonth") startOfMonth: LocalDateTime): Long

    // 전체 활성 세션 수 (SUPER_ADMIN용)
    fun countByIsActiveTrueAndExpiresAtAfter(expiresAt: LocalDateTime): Long

    // 전체 오늘 로그인 수 (SUPER_ADMIN용)
    fun countByCreatedAtAfter(createdAt: LocalDateTime): Long

    // 여러 사이트의 활성 세션 수 (일반 관리자용)
    @Query(
        """
        SELECT COUNT(s) FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId IN :siteIds AND s.isActive = true AND s.expiresAt > :expiresAt
    """
    )
    fun countBySiteIdInAndIsActiveTrueAndExpiresAtAfter(@Param("siteIds") siteIds: List<Long>, @Param("expiresAt") expiresAt: LocalDateTime): Long

    // 여러 사이트의 오늘 로그인 수 (일반 관리자용)
    @Query(
        """
        SELECT COUNT(s) FROM SsoSession s 
        INNER JOIN SsoUser su ON s.ssoUserId = su.id 
        WHERE su.siteId IN :siteIds AND s.createdAt >= :createdAt
    """
    )
    fun countBySiteIdInAndCreatedAtAfter(@Param("siteIds") siteIds: List<Long>, @Param("createdAt") createdAt: LocalDateTime): Long
}
