package com.comdeply.comment.app.web.svc

import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.SsoSession
import com.comdeply.comment.entity.SsoUser
import com.comdeply.comment.repository.SiteRepository
import com.comdeply.comment.repository.SsoSessionRepository
import com.comdeply.comment.repository.SsoUserRepository
import com.comdeply.comment.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
@Transactional
class SsoService(
    private val ssoUserRepository: SsoUserRepository,
    private val ssoSessionRepository: SsoSessionRepository,
    private val siteRepository: SiteRepository,
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
    @Value("\${app.jwt.secret:default-secret-key}") private val jwtSecret: String,
    @Value("\${app.jwt.expiration:3600}") private val jwtExpirationSeconds: Long
) {
    private val logger = LoggerFactory.getLogger(SsoService::class.java)

    /**
     * SSO 인증 처리
     */
    fun authenticateUser(request: SsoAuthRequest, ipAddress: String?, userAgent: String?): SsoAuthResponse {
        try {
            // 1. 사이트 검증
            val site = siteRepository.findById(request.siteId)
                .orElseThrow { IllegalArgumentException("존재하지 않는 사이트입니다") }

            // 2. 서명 검증
            if (!verifySignature(request, site.ssoSecretKey ?: "")) {
                return SsoAuthResponse(
                    success = false,
                    error = "서명 검증에 실패했습니다",
                    errorCode = "INVALID_SIGNATURE"
                )
            }

            // 3. 타임스탬프 검증 (5분 내)
            val currentTime = System.currentTimeMillis() / 1000
            if (Math.abs(currentTime - request.timestamp) > 300) {
                return SsoAuthResponse(
                    success = false,
                    error = "요청 시간이 만료되었습니다",
                    errorCode = "TIMESTAMP_EXPIRED"
                )
            }

            // 4. 사용자 정보 복호화
            val externalUserInfo = decryptUserToken(request.userToken, site.ssoSecretKey ?: "")

            // 5. ComDeply 사용자 생성 또는 조회
            val ssoUser = findOrCreateSsoUser(request.siteId, externalUserInfo)

            // 6. 세션 생성
            val session = createSession(ssoUser, ipAddress, userAgent)

            // 7. JWT 토큰 생성
            val accessToken = generateAccessToken(ssoUser, session)
            val refreshToken = generateRefreshToken(ssoUser, session)

            return SsoAuthResponse(
                success = true,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = jwtExpirationSeconds,
                user = SsoUserResponse(
                    id = ssoUser.userId,
                    name = ssoUser.externalUserName,
                    email = ssoUser.externalEmail,
                    profileImageUrl = ssoUser.externalProfileImageUrl,
                    externalUserId = ssoUser.externalUserId,
                    lastLoginAt = ssoUser.lastLoginAt
                )
            )
        } catch (e: Exception) {
            logger.error("SSO 인증 처리 중 오류 발생", e)
            return SsoAuthResponse(
                success = false,
                error = "인증 처리 중 오류가 발생했습니다",
                errorCode = "AUTHENTICATION_ERROR"
            )
        }
    }

    /**
     * 토큰 갱신
     */
    fun refreshToken(request: SsoRefreshRequest): SsoAuthResponse {
        try {
            // 1. 리프레시 토큰 검증
            val claims = validateJwtToken(request.refreshToken)
            val sessionId = claims["sessionId"]?.toString()?.toLong()
                ?: throw IllegalArgumentException("세션 ID가 없습니다")

            // 2. 세션 조회
            val session = ssoSessionRepository.findById(sessionId)
                .orElseThrow { IllegalArgumentException("세션을 찾을 수 없습니다") }

            if (!session.isActive || session.expiresAt.isBefore(LocalDateTime.now())) {
                throw IllegalArgumentException("만료된 세션입니다")
            }

            // 3. SSO 사용자 조회
            val ssoUser = ssoUserRepository.findById(session.ssoUserId)
                .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다") }

            // 4. 새 토큰 생성
            val newAccessToken = generateAccessToken(ssoUser, session)
            val newRefreshToken = generateRefreshToken(ssoUser, session)

            // 5. 세션 활동 시간 업데이트
            ssoSessionRepository.updateLastActivity(session.id, LocalDateTime.now())

            return SsoAuthResponse(
                success = true,
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                expiresIn = jwtExpirationSeconds,
                user = SsoUserResponse(
                    id = ssoUser.userId,
                    name = ssoUser.externalUserName,
                    email = ssoUser.externalEmail,
                    profileImageUrl = ssoUser.externalProfileImageUrl,
                    externalUserId = ssoUser.externalUserId,
                    lastLoginAt = ssoUser.lastLoginAt
                )
            )
        } catch (e: Exception) {
            logger.error("토큰 갱신 중 오류 발생", e)
            return SsoAuthResponse(
                success = false,
                error = "토큰 갱신에 실패했습니다",
                errorCode = "TOKEN_REFRESH_ERROR"
            )
        }
    }

    /**
     * 로그아웃
     */
    fun logout(sessionToken: String): Boolean {
        try {
            val session = ssoSessionRepository.findBySessionTokenAndIsActiveTrue(sessionToken)
                ?: return false

            ssoSessionRepository.deactivateSession(session.id)
            return true
        } catch (e: Exception) {
            logger.error("로그아웃 처리 중 오류 발생", e)
            return false
        }
    }

    /**
     * 사용자의 모든 세션 로그아웃
     */
    fun logoutAllSessions(ssoUserId: Long): Boolean {
        try {
            ssoSessionRepository.deactivateAllUserSessions(ssoUserId)
            return true
        } catch (e: Exception) {
            logger.error("전체 세션 로그아웃 처리 중 오류 발생", e)
            return false
        }
    }

    /**
     * 토큰 검증
     */
    fun validateToken(token: String): SsoUserResponse? {
        try {
            val claims = validateJwtToken(token)
            val ssoUserId = claims["ssoUserId"]?.toString()?.toLong()
                ?: return null

            val ssoUser = ssoUserRepository.findById(ssoUserId)
                .orElse(null) ?: return null

            if (!ssoUser.isActive) return null

            return SsoUserResponse(
                id = ssoUser.userId,
                name = ssoUser.externalUserName,
                email = ssoUser.externalEmail,
                profileImageUrl = ssoUser.externalProfileImageUrl,
                externalUserId = ssoUser.externalUserId,
                lastLoginAt = ssoUser.lastLoginAt
            )
        } catch (e: Exception) {
            logger.error("토큰 검증 중 오류 발생", e)
            return null
        }
    }

    /**
     * 사이트별 SSO 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    fun getSsoUsers(siteId: Long, page: Int, size: Int): SsoUserListResponse {
        val pageRequest = PageRequest.of(page, size)
        val result = ssoUserRepository.findBySiteIdAndIsActiveTrueOrderByLastLoginAtDesc(siteId, pageRequest)

        val users = result.content.map { ssoUser ->
            SsoUserResponse(
                id = ssoUser.userId,
                name = ssoUser.externalUserName,
                email = ssoUser.externalEmail,
                profileImageUrl = ssoUser.externalProfileImageUrl,
                externalUserId = ssoUser.externalUserId,
                lastLoginAt = ssoUser.lastLoginAt
            )
        }

        return SsoUserListResponse(
            users = users,
            totalCount = result.totalElements,
            currentPage = page,
            pageSize = size
        )
    }

    /**
     * 사이트별 SSO 통계 조회
     */
    @Transactional(readOnly = true)
    fun getSsoStats(siteId: Long): SsoStatsResponse {
        val now = LocalDateTime.now()
        val startOfDay = now.toLocalDate().atStartOfDay()
        val startOfWeek = startOfDay.minusDays(now.dayOfWeek.value - 1L)
        val startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay()
        val weekAgo = now.minusDays(7)

        val totalSsoUsers = ssoUserRepository.countBySiteIdAndIsActiveTrue(siteId)
        val activeSessions = ssoSessionRepository.countActiveSessions(siteId)
        val todayLogins = ssoUserRepository.countTodayLogins(siteId, startOfDay)
        val weeklyLogins = ssoUserRepository.countWeeklyLogins(siteId, startOfWeek)
        val monthlyLogins = ssoUserRepository.countMonthlyLogins(siteId, startOfMonth)

        val avgSessionDuration = ssoSessionRepository.getAvgSessionDurationMinutes(siteId, weekAgo) ?: 0.0

        // 브라우저 통계
        val browserStats = ssoSessionRepository.getBrowserStats(siteId, weekAgo)
            .associate {
                val userAgent = it[0]?.toString() ?: "Unknown"
                val browser = extractBrowserName(userAgent)
                val count = (it[1] as? Number)?.toLong() ?: 0L
                browser to count
            }

        // 일별 로그인 통계
        val dailyLoginStats = ssoUserRepository.getDailyLoginStats(siteId, weekAgo)
            .associate {
                val date = it[0]?.toString() ?: ""
                val count = (it[1] as? Number)?.toLong() ?: 0L
                date to count
            }

        return SsoStatsResponse(
            totalSsoUsers = totalSsoUsers,
            activeSessions = activeSessions,
            todayLogins = todayLogins,
            weeklyLogins = weeklyLogins,
            monthlyLogins = monthlyLogins,
            avgSessionDuration = avgSessionDuration,
            browserStats = browserStats,
            dailyLoginStats = dailyLoginStats
        )
    }

    /**
     * 만료된 세션 정리
     */
    @Transactional
    fun cleanupExpiredSessions(): Int {
        return ssoSessionRepository.deactivateExpiredSessions(LocalDateTime.now())
    }

    // === Private Methods ===

    private fun verifySignature(request: SsoAuthRequest, secretKey: String): Boolean {
        val dataToSign = "${request.siteId}|${request.userToken}|${request.timestamp}"
        val expectedSignature = createHmacSignature(dataToSign, secretKey)
        return request.signature == expectedSignature
    }

    private fun createHmacSignature(data: String, secretKey: String): String {
        val keySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        val signature = mac.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(signature)
    }

    private fun decryptUserToken(userToken: String, secretKey: String): ExternalUserInfo {
        // Base64 디코딩
        val decodedBytes = Base64.getDecoder().decode(userToken)
        val jsonString = String(decodedBytes)

        // JSON 파싱
        return objectMapper.readValue(jsonString, ExternalUserInfo::class.java)
    }

    private fun findOrCreateSsoUser(siteId: Long, externalUserInfo: ExternalUserInfo): SsoUser {
        // 기존 SSO 사용자 조회
        val existingSsoUser = ssoUserRepository.findBySiteIdAndExternalUserId(siteId, externalUserInfo.userId)

        if (existingSsoUser != null) {
            // 기존 사용자 정보 업데이트
            val updatedSsoUser = existingSsoUser.copy(
                externalUserName = externalUserInfo.name,
                externalEmail = externalUserInfo.email,
                externalProfileImageUrl = externalUserInfo.profileImageUrl,
                externalMetadata = externalUserInfo.metadata?.let { objectMapper.writeValueAsString(it) },
                lastLoginAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            return ssoUserRepository.save(updatedSsoUser)
        }

        // 새 ComDeply 사용자 생성
        val newUser = userService.createAnonymousUser()

        // 새 SSO 사용자 생성
        val newSsoUser = SsoUser(
            siteId = siteId,
            userId = newUser.id,
            externalUserId = externalUserInfo.userId,
            externalUserName = externalUserInfo.name,
            externalEmail = externalUserInfo.email,
            externalProfileImageUrl = externalUserInfo.profileImageUrl,
            externalMetadata = externalUserInfo.metadata?.let { objectMapper.writeValueAsString(it) }
        )

        return ssoUserRepository.save(newSsoUser)
    }

    private fun createSession(ssoUser: SsoUser, ipAddress: String?, userAgent: String?): SsoSession {
        val expiresAt = LocalDateTime.now().plusSeconds(jwtExpirationSeconds * 2) // 리프레시 토큰은 2배 시간

        val session = SsoSession(
            ssoUserId = ssoUser.id,
            sessionToken = UUID.randomUUID().toString(),
            ipAddress = ipAddress,
            userAgent = userAgent,
            expiresAt = expiresAt
        )

        return ssoSessionRepository.save(session)
    }

    private fun generateAccessToken(ssoUser: SsoUser, session: SsoSession): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationSeconds * 1000)

        return Jwts.builder()
            .setSubject(ssoUser.userId.toString())
            .claim("ssoUserId", ssoUser.id)
            .claim("sessionId", session.id)
            .claim("siteId", ssoUser.siteId)
            .claim("externalUserId", ssoUser.externalUserId)
            .claim("name", ssoUser.externalUserName)
            .claim("email", ssoUser.externalEmail)
            .claim("type", "access")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(Keys.hmacShaKeyFor(jwtSecret.toByteArray()), SignatureAlgorithm.HS256)
            .compact()
    }

    private fun generateRefreshToken(ssoUser: SsoUser, session: SsoSession): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationSeconds * 2 * 1000) // 2배 시간

        return Jwts.builder()
            .setSubject(ssoUser.userId.toString())
            .claim("ssoUserId", ssoUser.id)
            .claim("sessionId", session.id)
            .claim("siteId", ssoUser.siteId)
            .claim("type", "refresh")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(Keys.hmacShaKeyFor(jwtSecret.toByteArray()), SignatureAlgorithm.HS256)
            .compact()
    }

    private fun validateJwtToken(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.toByteArray()))
            .build()
            .parseClaimsJws(token)
            .body
    }

    private fun extractBrowserName(userAgent: String): String {
        return when {
            userAgent.contains("Chrome") -> "Chrome"
            userAgent.contains("Firefox") -> "Firefox"
            userAgent.contains("Safari") -> "Safari"
            userAgent.contains("Edge") -> "Edge"
            else -> "Other"
        }
    }
}
