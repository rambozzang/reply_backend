package com.comdeply.comment.config

import com.comdeply.comment.app.web.svc.SiteService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.io.BufferedReader
import java.net.URI

@Component
class DomainValidationInterceptor(
    private val siteService: SiteService,
    private val objectMapper: ObjectMapper
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        // 댓글 관련 API만 검증
        if (shouldValidateDomain(request)) {
            if (!validateDomain(request)) {
                response.status = HttpServletResponse.SC_FORBIDDEN
                response.contentType = "application/json"
                response.writer.write("{\"error\": \"Domain not allowed\", \"message\": \"요청한 도메인이 등록된 도메인과 일치하지 않습니다.\"}")
                return false
            }
        }
        return true
    }

    private fun shouldValidateDomain(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        val method = request.method

        // 댓글 생성, 조회, 수정, 삭제 API 검증
        return when {
            path.startsWith("/comments") && method in listOf("POST", "GET", "PUT", "DELETE") -> true
            path.startsWith("/embed/config") && method == "GET" -> true
            path.startsWith("/auth/v2/guest/comment") && method == "POST" -> true
            else -> false
        }
    }

    private fun validateDomain(request: HttpServletRequest): Boolean {
        try {
            // Origin 헤더 확인 (CORS 요청)
            val origin = request.getHeader("Origin")
            // Referer 헤더 확인 (일반 요청)
            val referer = request.getHeader("Referer")

            val requestDomain =
                when {
                    origin != null -> extractDomain(origin)
                    referer != null -> extractDomain(referer)
                    else -> {
                        // 로컬 테스트나 직접 API 호출은 허용 (개발용)
                        val userAgent = request.getHeader("User-Agent") ?: ""
                        return userAgent.contains("curl") ||
                            userAgent.contains("Postman") ||
                            request.remoteAddr in listOf("127.0.0.1", "::1", "localhost")
                    }
                }

            if (requestDomain == null) return false

            // 사이트 ID 추출
            val siteKey = extractSiteId(request) ?: return false

            // 등록된 도메인과 비교
            val site =
                try {
                    // 먼저 shortName으로 조회
                    siteService.findBySiteKey(siteKey)
                        ?: // 숫자 ID로 조회
                        siteKey.toLongOrNull()?.let { id ->
                            siteService.getSite(id).let { response ->
                                siteService.findBySiteKey(response.siteKey)
                            }
                        }
                } catch (e: Exception) {
                    null
                }

            if (site == null) return false

            // 도메인 검증 - 허용 도메인 목록 또는 기본 도메인 사용
            val allowedDomains = site.allowedDomains?.let { it } ?: listOf(site.domain)

            return allowedDomains.any { allowedDomain ->
                isDomainAllowed(requestDomain, allowedDomain)
            }
        } catch (e: Exception) {
            return false
        }
    }

    private fun extractDomain(url: String): String? =
        try {
            val uri = URI(url)
            uri.host?.lowercase()
        } catch (e: Exception) {
            null
        }

    private fun extractSiteId(request: HttpServletRequest): String? {
        // URL 파라미터에서 siteId 추출
        request.getParameter("siteKey")?.let { return it }

        // POST 요청의 경우 body에서 추출
        if (request.method == "POST") {
            try {
                val body = request.reader.use(BufferedReader::readText)
                if (body.isNotEmpty()) {
                    val jsonNode = objectMapper.readTree(body)
                    jsonNode.get("siteKey")?.asText()?.let { return it }
                }
            } catch (e: Exception) {
                // body 읽기 실패 시 무시
            }
        }

        return null
    }

    private fun isDomainAllowed(
        requestDomain: String,
        allowedDomain: String
    ): Boolean {
        val cleanAllowedDomain =
            allowedDomain
                .lowercase()
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("www.")
                .split("/")[0] // 경로 제거

        val cleanRequestDomain =
            requestDomain
                .lowercase()
                .removePrefix("www.")

        return when {
            // 정확히 일치
            cleanRequestDomain == cleanAllowedDomain -> true
            // 서브도메인 허용 (예: blog.example.com이 example.com에 포함)
            cleanRequestDomain.endsWith(".$cleanAllowedDomain") -> true
            // 로컬 개발 환경 허용
            cleanRequestDomain in listOf("localhost", "127.0.0.1") -> true
            else -> false
        }
    }
}
