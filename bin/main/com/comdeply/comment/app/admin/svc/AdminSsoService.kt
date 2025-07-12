package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.app.admin.svc.vo.SsoAllSessionsTerminationResponse
import com.comdeply.comment.app.admin.svc.vo.SsoHmacKeyRegenerationResponse
import com.comdeply.comment.app.admin.svc.vo.SsoSessionTerminationResponse
import com.comdeply.comment.app.admin.svc.vo.SsoUserDeactivationResponse
import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.SitePermission
import com.comdeply.comment.entity.SsoSession
import com.comdeply.comment.entity.SsoUser
import com.comdeply.comment.repository.SiteRepository
import com.comdeply.comment.repository.SsoSessionRepository
import com.comdeply.comment.repository.SsoUserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
@Transactional
class AdminSsoService(
    private val ssoUserRepository: SsoUserRepository,
    private val ssoSessionRepository: SsoSessionRepository,
    private val siteRepository: SiteRepository,
    private val adminPermissionService: AdminPermissionService,
    private val objectMapper: ObjectMapper,
    @Value("\${app.base-url:http://localhost:8080}") private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(AdminSsoService::class.java)

    /**
     * SSO 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    fun getSsoUsers(siteId: Long, admin: Admin, page: Int, size: Int): Page<SsoUserResponse> {
        logger.info("SSO 사용자 목록 조회: siteId={}, adminId={}, page={}, size={}", siteId, admin.id, page, size)

        // 해당 사이트에 대한 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        try {
            val pageable = PageRequest.of(page, size)
            val ssoUsersPage = ssoUserRepository.findBySiteIdAndIsActiveTrueOrderByLastLoginAtDesc(siteId, pageable)

            val responsePage = ssoUsersPage.map { ssoUser ->
                convertToSsoUserResponse(ssoUser)
            }

            logger.info("SSO 사용자 목록 조회 완료: siteId={}, 총 {}개", siteId, responsePage.totalElements)
            return responsePage
        } catch (e: Exception) {
            logger.error("SSO 사용자 목록 조회 중 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * SSO 세션 목록 조회
     */
    @Transactional(readOnly = true)
    fun getSsoSessions(siteId: Long, admin: Admin, page: Int, size: Int): Page<SsoSessionResponse> {
        logger.info("SSO 세션 목록 조회: siteId={}, adminId={}, page={}, size={}", siteId, admin.id, page, size)

        // 해당 사이트에 대한 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        try {
            val pageable = PageRequest.of(page, size)
            val sessionsPage = ssoSessionRepository.findBySiteIdAndIsActiveTrueOrderByCreatedAtDescPaged(siteId, pageable)

            val responsePage = sessionsPage.map { session ->
                convertToSsoSessionResponse(session)
            }

            logger.info("SSO 세션 목록 조회 완료: siteId={}, 총 {}개", siteId, responsePage.totalElements)
            return responsePage
        } catch (e: Exception) {
            logger.error("SSO 세션 목록 조회 중 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * SSO 통계 조회
     */
    @Transactional(readOnly = true)
    fun getSsoStats(siteId: Long, admin: Admin): SsoStatsResponse {
        logger.info("SSO 통계 조회: siteId={}, adminId={}", siteId, admin.id)

        // 해당 사이트에 대한 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        try {
            val now = LocalDateTime.now()

            // 전체 SSO 사용자 수
            val totalSsoUsers = ssoUserRepository.countBySiteIdAndIsActiveTrue(siteId)

            // 활성 세션 수
            val activeSessions = ssoSessionRepository.countActiveSessions(siteId)

            // 오늘 로그인 수
            val startOfDay = now.toLocalDate().atStartOfDay()
            val todayLogins = ssoSessionRepository.countTodayLogins(siteId, startOfDay)

            // 이번 주 로그인 수
            val startOfWeek = now.toLocalDate().minusDays(now.dayOfWeek.value - 1L).atStartOfDay()
            val weeklyLogins = ssoSessionRepository.countWeeklyLogins(siteId, startOfWeek)

            // 이번 달 로그인 수
            val startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay()
            val monthlyLogins = ssoSessionRepository.countMonthlyLogins(siteId, startOfMonth)

            val response = SsoStatsResponse(
                totalSsoUsers = totalSsoUsers,
                activeSessions = activeSessions,
                todayLogins = todayLogins,
                weeklyLogins = weeklyLogins,
                monthlyLogins = monthlyLogins,
                avgSessionDuration = 45.0, // TODO: 실제 계산 로직 구현
                browserStats = emptyMap(), // TODO: 브라우저 통계 구현
                dailyLoginStats = emptyMap() // TODO: 일별 로그인 통계 구현
            )

            logger.info(
                "SSO 통계 조회 완료: siteId={}, totalUsers={}, activeSessions={}",
                siteId,
                totalSsoUsers,
                activeSessions
            )
            return response
        } catch (e: Exception) {
            logger.error("SSO 통계 조회 중 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * SSO 사용자 비활성화
     */
    fun deactivateSsoUser(ssoUserId: Long, admin: Admin): SsoUserDeactivationResponse {
        logger.info("SSO 사용자 비활성화: ssoUserId={}, adminId={}", ssoUserId, admin.id)

        val ssoUser = ssoUserRepository.findById(ssoUserId)
            .orElseThrow { IllegalArgumentException("SSO 사용자를 찾을 수 없습니다: $ssoUserId") }

        // 해당 사이트에 대한 관리 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, ssoUser.siteId, SitePermission.MANAGE)) {
            throw IllegalArgumentException("해당 사이트에 대한 관리 권한이 없습니다")
        }

        try {
            val updatedSsoUser = ssoUser.copy(
                isActive = false,
                updatedAt = LocalDateTime.now()
            )
            ssoUserRepository.save(updatedSsoUser)

            // 해당 사용자의 모든 활성 세션 종료
            val activeSessions = ssoSessionRepository.findBySsoUserIdAndIsActiveTrueOrderByCreatedAtDesc(ssoUserId)
            var terminatedSessionCount = 0

            activeSessions.forEach { session: SsoSession ->
                val updatedSession = session.copy(isActive = false)
                ssoSessionRepository.save(updatedSession)
                terminatedSessionCount++
            }

            val response = SsoUserDeactivationResponse(
                ssoUserId = ssoUserId,
                username = ssoUser.externalUserName,
                terminatedSessionCount = terminatedSessionCount,
                message = "SSO 사용자가 성공적으로 비활성화되었습니다",
                deactivatedAt = System.currentTimeMillis()
            )

            logger.info(
                "SSO 사용자 비활성화 완료: ssoUserId={}, username={}, terminatedSessions={}",
                ssoUserId,
                ssoUser.externalUserName,
                terminatedSessionCount
            )
            return response
        } catch (e: Exception) {
            logger.error("SSO 사용자 비활성화 중 오류 발생: ssoUserId={}", ssoUserId, e)
            throw e
        }
    }

    /**
     * SSO 세션 종료
     */
    fun terminateSession(sessionId: Long, admin: Admin): SsoSessionTerminationResponse {
        logger.info("SSO 세션 종료: sessionId={}, adminId={}", sessionId, admin.id)

        val session = ssoSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("SSO 세션을 찾을 수 없습니다: $sessionId") }

        // 해당 사이트에 대한 관리 권한 확인
        val siteId = session.ssoUser?.siteId ?: throw IllegalArgumentException("세션의 사이트 정보를 찾을 수 없습니다")
        if (!adminPermissionService.hasPermissionForSite(admin, siteId, SitePermission.MANAGE)) {
            throw IllegalArgumentException("해당 사이트에 대한 관리 권한이 없습니다")
        }

        try {
            val updatedSession = session.copy(isActive = false)
            ssoSessionRepository.save(updatedSession)

            val response = SsoSessionTerminationResponse(
                sessionId = sessionId,
                ssoUserId = session.ssoUserId,
                username = session.ssoUser?.externalUserName ?: "Unknown",
                message = "SSO 세션이 성공적으로 종료되었습니다",
                terminatedAt = System.currentTimeMillis()
            )

            logger.info("SSO 세션 종료 완료: sessionId={}, ssoUserId={}", sessionId, session.ssoUserId)
            return response
        } catch (e: Exception) {
            logger.error("SSO 세션 종료 중 오류 발생: sessionId={}", sessionId, e)
            throw e
        }
    }

    /**
     * 모든 SSO 세션 종료
     */
    fun terminateAllSessions(siteId: Long, admin: Admin): SsoAllSessionsTerminationResponse {
        logger.info("모든 SSO 세션 종료: siteId={}, adminId={}", siteId, admin.id)

        // 해당 사이트에 대한 관리 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId, SitePermission.MANAGE)) {
            throw IllegalArgumentException("해당 사이트에 대한 관리 권한이 없습니다")
        }

        try {
            val activeSessions = ssoSessionRepository.findBySiteIdAndIsActiveTrueOrderByCreatedAtDesc(siteId)
            var terminatedCount = 0

            activeSessions.forEach { session: SsoSession ->
                val updatedSession = session.copy(isActive = false)
                ssoSessionRepository.save(updatedSession)
                terminatedCount++
            }

            val response = SsoAllSessionsTerminationResponse(
                siteId = siteId,
                terminatedCount = terminatedCount,
                message = "${terminatedCount}개의 SSO 세션이 성공적으로 종료되었습니다",
                terminatedAt = System.currentTimeMillis()
            )

            logger.info("모든 SSO 세션 종료 완료: siteId={}, terminatedCount={}", siteId, terminatedCount)
            return response
        } catch (e: Exception) {
            logger.error("모든 SSO 세션 종료 중 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * SSO 설정 업데이트
     */
    fun updateSsoConfig(siteId: Long, request: SsoConfigRequest, admin: Admin): SsoConfigResponse {
        logger.info("SSO 설정 업데이트: siteId={}, enabled={}, adminId={}", siteId, request.enabled, admin.id)

        // 해당 사이트에 대한 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        try {
            val site = siteRepository.findById(siteId)
                .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다: $siteId") }

            // HMAC 시크릿 키 생성 (없는 경우) 또는 기존 키 사용
            val hmacSecretKey = request.hmacSecretKey?.takeIf { it.isNotBlank() }
                ?: site.ssoSecretKey?.takeIf { it.isNotBlank() }
                ?: generateSecretKey()

            // 허용된 도메인 목록 JSON 변환
            val allowedDomainsJson = request.allowedDomains?.let { domains ->
                objectMapper.writeValueAsString(domains)
            }

            // 사이트 SSO 설정 업데이트
            val updatedSite = site.copy(
                ssoEnabled = request.enabled,
                ssoSecretKey = hmacSecretKey,
                ssoTokenExpirationMinutes = request.tokenExpirationMinutes,
                ssoAllowedDomains = allowedDomainsJson,
                updatedAt = LocalDateTime.now()
            )

            siteRepository.save(updatedSite)

            val response = SsoConfigResponse(
                siteId = siteId,
                enabled = request.enabled,
                hmacSecretKey = hmacSecretKey,
                tokenExpirationMinutes = request.tokenExpirationMinutes,
                allowedDomains = request.allowedDomains,
                ssoEndpoint = "$baseUrl/api/sso/auth",
                updatedAt = LocalDateTime.now()
            )

            logger.info("SSO 설정 업데이트 완료: siteId={}, enabled={}", siteId, request.enabled)
            return response
        } catch (e: Exception) {
            logger.error("SSO 설정 업데이트 중 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * SSO 설정 조회
     */
    @Transactional(readOnly = true)
    fun getSsoConfig(siteId: Long, admin: Admin): SsoConfigResponse {
        logger.info("SSO 설정 조회: siteId={}, adminId={}", siteId, admin.id)

        // 해당 사이트에 대한 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        try {
            val site = siteRepository.findById(siteId)
                .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다: $siteId") }

            // 허용된 도메인 목록 JSON 파싱
            val allowedDomains = site.ssoAllowedDomains?.let { domainsJson ->
                try {
                    objectMapper
                        .readValue(domainsJson, Array<String>::class.java)
                        .toList()
                } catch (e: Exception) {
                    logger.warn("허용된 도메인 목록 파싱 실패: {}", e.message)
                    null
                }
            }

            val response = SsoConfigResponse(
                siteId = siteId,
                enabled = site.ssoEnabled,
                hmacSecretKey = site.ssoSecretKey ?: "",
                tokenExpirationMinutes = site.ssoTokenExpirationMinutes ?: 60,
                allowedDomains = allowedDomains,
                ssoEndpoint = "$baseUrl/api/sso/auth",
                updatedAt = site.updatedAt
            )

            logger.info("SSO 설정 조회 완료: siteId={}, enabled={}", siteId, site.ssoEnabled)
            return response
        } catch (e: Exception) {
            logger.error("SSO 설정 조회 중 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * HMAC 키 재생성
     */
    fun regenerateHmacKey(siteId: Long, admin: Admin): SsoHmacKeyRegenerationResponse {
        logger.info("HMAC 키 재생성: siteId={}, adminId={}", siteId, admin.id)

        // 해당 사이트에 대한 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        try {
            val site = siteRepository.findById(siteId)
                .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다: $siteId") }

            val newSecretKey = generateSecretKey()

            val updatedSite = site.copy(
                ssoSecretKey = newSecretKey,
                updatedAt = LocalDateTime.now()
            )

            siteRepository.save(updatedSite)

            val response = SsoHmacKeyRegenerationResponse(
                siteId = siteId,
                newSecretKey = newSecretKey,
                warning = "기존 키로 생성된 토큰은 더 이상 유효하지 않습니다",
                regeneratedAt = System.currentTimeMillis()
            )

            logger.info("HMAC 키 재생성 완료: siteId={}", siteId)
            return response
        } catch (e: Exception) {
            logger.error("HMAC 키 재생성 중 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * 비밀 키 생성
     */
    private fun generateSecretKey(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32) // 256-bit key
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * HMAC 서명 생성
     */
    private fun createHmacSignature(data: String, secretKey: String): String {
        val keySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        val signature = mac.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(signature)
    }

    /**
     * SsoUser 엔티티를 SsoUserResponse DTO로 변환
     */
    private fun convertToSsoUserResponse(ssoUser: SsoUser): SsoUserResponse {
        return SsoUserResponse(
            id = ssoUser.id,
            name = ssoUser.externalUserName,
            email = ssoUser.externalEmail,
            profileImageUrl = null, // TODO: 프로필 이미지 지원 추가
            externalUserId = ssoUser.externalUserId,
            lastLoginAt = ssoUser.lastLoginAt
        )
    }

    /**
     * SsoSession 엔티티를 SsoSessionResponse DTO로 변환
     */
    private fun convertToSsoSessionResponse(session: SsoSession): SsoSessionResponse {
        return SsoSessionResponse(
            id = session.id,
            ssoUserId = session.ssoUserId,
            userName = session.ssoUser?.externalUserName ?: "Unknown",
            sessionToken = session.sessionToken,
            isActive = session.isActive,
            expiresAt = session.expiresAt,
            createdAt = session.createdAt,
            lastActivityAt = session.lastActivityAt,
            ipAddress = session.ipAddress,
            userAgent = session.userAgent
        )
    }
}
