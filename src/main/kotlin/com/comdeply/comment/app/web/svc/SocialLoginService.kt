package com.comdeply.comment.app.web.svc

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.collections.get

@Service
class SocialLoginService(
    private val environment: Environment,
    @Value("\${spring.config.location:./}") private val configLocation: String
) {
    private val logger = LoggerFactory.getLogger(SocialLoginService::class.java)

    private val configFile = "application-oauth.properties"
    private val configPath = Paths.get(configLocation, configFile)

    fun getSettings(): Map<String, Any> {
        logger.info("소셜 로그인 설정 조회")

        val settings = mapOf(
            "google" to mapOf(
                "enabled" to (environment.getProperty("spring.security.oauth2.client.registration.google.enabled", "false").toBoolean()),
                "clientId" to (environment.getProperty("spring.security.oauth2.client.registration.google.client-id") ?: ""),
                "clientSecret" to if (environment.getProperty("spring.security.oauth2.client.registration.google.client-secret").isNullOrBlank()) "" else "****",
                "redirectUri" to (environment.getProperty("spring.security.oauth2.client.registration.google.redirect-uri") ?: "")
            ),
            "kakao" to mapOf(
                "enabled" to (environment.getProperty("spring.security.oauth2.client.registration.kakao.enabled", "false").toBoolean()),
                "clientId" to (environment.getProperty("spring.security.oauth2.client.registration.kakao.client-id") ?: ""),
                "clientSecret" to if (environment.getProperty("spring.security.oauth2.client.registration.kakao.client-secret").isNullOrBlank()) "" else "****",
                "redirectUri" to (environment.getProperty("spring.security.oauth2.client.registration.kakao.redirect-uri") ?: "")
            ),
            "naver" to mapOf(
                "enabled" to (environment.getProperty("spring.security.oauth2.client.registration.naver.enabled", "false").toBoolean()),
                "clientId" to (environment.getProperty("spring.security.oauth2.client.registration.naver.client-id") ?: ""),
                "clientSecret" to if (environment.getProperty("spring.security.oauth2.client.registration.naver.client-secret").isNullOrBlank()) "" else "****",
                "redirectUri" to (environment.getProperty("spring.security.oauth2.client.registration.naver.redirect-uri") ?: "")
            )
        )

        return settings
    }

    fun updateSettings(settings: Map<String, Any>): Map<String, Any> {
        logger.info("소셜 로그인 설정 업데이트")

        try {
            val properties = Properties()

            // 기존 설정 파일이 있으면 로드
            if (Files.exists(configPath)) {
                Files.newInputStream(configPath).use { inputStream ->
                    properties.load(inputStream)
                }
            }

            // Google 설정 업데이트
            val googleSettings = settings["google"] as? Map<*, *>
            if (googleSettings != null) {
                updateProviderProperties(properties, "google", googleSettings)
            }

            // Kakao 설정 업데이트
            val kakaoSettings = settings["kakao"] as? Map<*, *>
            if (kakaoSettings != null) {
                updateProviderProperties(properties, "kakao", kakaoSettings)
            }

            // Naver 설정 업데이트
            val naverSettings = settings["naver"] as? Map<*, *>
            if (naverSettings != null) {
                updateProviderProperties(properties, "naver", naverSettings)
            }

            // 설정 파일 저장
            savePropertiesToFile(properties)

            logger.info("소셜 로그인 설정 업데이트 완료")

            // 업데이트된 설정 반환 (마스킹된 버전)
            return getSettings()
        } catch (e: Exception) {
            logger.error("소셜 로그인 설정 업데이트 실패", e)
            throw RuntimeException("설정 업데이트에 실패했습니다: ${e.message}")
        }
    }

    fun updateProvider(provider: String, settings: Map<String, Any>): Map<String, Any> {
        logger.info("소셜 로그인 제공자 설정 업데이트: provider={}", provider)

        if (provider !in listOf("google", "kakao", "naver")) {
            throw IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: $provider")
        }

        val fullSettings = mapOf(provider to settings)
        return updateSettings(fullSettings)
    }

    fun testProvider(provider: String): Map<String, Any> {
        logger.info("소셜 로그인 제공자 설정 테스트: provider={}", provider)

        try {
            val clientId = environment.getProperty("spring.security.oauth2.client.registration.$provider.client-id")
            val clientSecret = environment.getProperty("spring.security.oauth2.client.registration.$provider.client-secret")
            val enabled = environment.getProperty("spring.security.oauth2.client.registration.$provider.enabled", "false").toBoolean()

            val isValid = when {
                !enabled -> false
                clientId.isNullOrBlank() -> false
                provider != "kakao" && clientSecret.isNullOrBlank() -> false // 카카오는 client-secret이 선택사항
                else -> true
            }

            val message = when {
                !enabled -> "$provider 로그인이 비활성화되어 있습니다"
                clientId.isNullOrBlank() -> "클라이언트 ID가 설정되지 않았습니다"
                provider != "kakao" && clientSecret.isNullOrBlank() -> "클라이언트 시크릿이 설정되지 않았습니다"
                else -> "$provider 설정이 올바르게 구성되어 있습니다"
            }

            return mapOf(
                "isValid" to isValid,
                "message" to message,
                "provider" to provider
            )
        } catch (e: Exception) {
            logger.error("소셜 로그인 제공자 설정 테스트 실패", e)
            return mapOf(
                "isValid" to false,
                "message" to "설정 테스트 중 오류가 발생했습니다: ${e.message}",
                "provider" to provider
            )
        }
    }

    fun resetSettings(): Map<String, Any> {
        logger.info("소셜 로그인 설정 초기화")

        try {
            val properties = Properties()

            // 모든 OAuth 설정 제거
            listOf("google", "kakao", "naver").forEach { provider ->
                properties.remove("spring.security.oauth2.client.registration.$provider.enabled")
                properties.remove("spring.security.oauth2.client.registration.$provider.client-id")
                properties.remove("spring.security.oauth2.client.registration.$provider.client-secret")
                properties.remove("spring.security.oauth2.client.registration.$provider.redirect-uri")
                properties.remove("spring.security.oauth2.client.registration.$provider.scope")
                properties.remove("spring.security.oauth2.client.registration.$provider.authorization-grant-type")
                properties.remove("spring.security.oauth2.client.provider.$provider.authorization-uri")
                properties.remove("spring.security.oauth2.client.provider.$provider.token-uri")
                properties.remove("spring.security.oauth2.client.provider.$provider.user-info-uri")
                properties.remove("spring.security.oauth2.client.provider.$provider.user-name-attribute")
            }

            // 설정 파일 저장
            savePropertiesToFile(properties)

            logger.info("소셜 로그인 설정 초기화 완료")

            return getSettings()
        } catch (e: Exception) {
            logger.error("소셜 로그인 설정 초기화 실패", e)
            throw RuntimeException("설정 초기화에 실패했습니다: ${e.message}")
        }
    }

    private fun updateProviderProperties(properties: Properties, provider: String, settings: Map<*, *>) {
        val enabled = settings["enabled"] as? Boolean ?: false
        val clientId = settings["clientId"] as? String ?: ""
        val clientSecret = settings["clientSecret"] as? String ?: ""
        val redirectUri = settings["redirectUri"] as? String ?: ""

        // 기본 설정
        properties.setProperty("spring.security.oauth2.client.registration.$provider.enabled", enabled.toString())

        if (enabled && clientId.isNotBlank()) {
            properties.setProperty("spring.security.oauth2.client.registration.$provider.client-id", clientId)

            // 클라이언트 시크릿이 마스킹되지 않은 경우에만 업데이트
            if (clientSecret.isNotBlank() && clientSecret != "****") {
                properties.setProperty("spring.security.oauth2.client.registration.$provider.client-secret", clientSecret)
            }

            if (redirectUri.isNotBlank()) {
                properties.setProperty("spring.security.oauth2.client.registration.$provider.redirect-uri", redirectUri)
            }

            // 제공자별 기본 설정
            when (provider) {
                "google" -> {
                    properties.setProperty("spring.security.oauth2.client.registration.$provider.scope", "profile,email")
                    properties.setProperty("spring.security.oauth2.client.registration.$provider.authorization-grant-type", "authorization_code")
                }
                "kakao" -> {
                    properties.setProperty("spring.security.oauth2.client.registration.$provider.scope", "profile_nickname,profile_image,account_email")
                    properties.setProperty("spring.security.oauth2.client.registration.$provider.authorization-grant-type", "authorization_code")
                    properties.setProperty("spring.security.oauth2.client.provider.$provider.authorization-uri", "https://kauth.kakao.com/oauth/authorize")
                    properties.setProperty("spring.security.oauth2.client.provider.$provider.token-uri", "https://kauth.kakao.com/oauth/token")
                    properties.setProperty("spring.security.oauth2.client.provider.$provider.user-info-uri", "https://kapi.kakao.com/v2/user/me")
                    properties.setProperty("spring.security.oauth2.client.provider.$provider.user-name-attribute", "id")
                }
                "naver" -> {
                    properties.setProperty("spring.security.oauth2.client.registration.$provider.scope", "name,email,profile_image")
                    properties.setProperty("spring.security.oauth2.client.registration.$provider.authorization-grant-type", "authorization_code")
                    properties.setProperty("spring.security.oauth2.client.provider.$provider.authorization-uri", "https://nid.naver.com/oauth2.0/authorize")
                    properties.setProperty("spring.security.oauth2.client.provider.$provider.token-uri", "https://nid.naver.com/oauth2.0/token")
                    properties.setProperty("spring.security.oauth2.client.provider.$provider.user-info-uri", "https://openapi.naver.com/v1/nid/me")
                    properties.setProperty("spring.security.oauth2.client.provider.$provider.user-name-attribute", "response")
                }
            }
        }
    }

    private fun savePropertiesToFile(properties: Properties) {
        try {
            // 디렉터리가 없으면 생성
            Files.createDirectories(configPath.parent)

            Files.newOutputStream(configPath).use { outputStream ->
                properties.store(outputStream, "Social Login OAuth2 Configuration - Generated by ComDeply Admin")
            }

            logger.info("설정 파일 저장 완료: {}", configPath)
        } catch (e: Exception) {
            logger.error("설정 파일 저장 실패", e)
            throw RuntimeException("설정 파일 저장에 실패했습니다: ${e.message}")
        }
    }
}
