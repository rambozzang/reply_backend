package com.comdeply.comment.app.web.svc

import com.comdeply.comment.dto.common.ValidationResult
import org.springframework.stereotype.Service
import java.net.URI
import java.util.regex.Pattern

@Service
class DomainValidationService(
    private val planValidationService: PlanValidationService
) {

    companion object {
        private val DOMAIN_PATTERN = Pattern.compile(
            "^([a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?[.])+[a-zA-Z]{2,}$"
        )
        private val FORBIDDEN_PATTERNS = listOf(
            "*", // 와일드카드 전체
            "*.com",
            "*.net",
            "*.org", // 광범위한 와일드카드
            "evil",
            "hack",
            "spam",
            "abuse" // 의심스러운 키워드
        )
    }

    /**
     * 도메인 목록 유효성 검증 (플랜별 제한 적용)
     */
    fun validateDomainList(domains: List<String>, userId: Long, currentDomainCount: Int = 0): ValidationResult {
        // 1. 플랜별 개수 제한 확인
        val planValidation = planValidationService.canAddDomains(userId, currentDomainCount, domains.size)
        if (!planValidation.isValid) {
            return ValidationResult.error(planValidation.message)
        }

        // 2. 각 도메인 검증
        domains.forEach { domain ->
            val cleanDomain = cleanDomain(domain)

            // 빈 도메인 확인
            if (cleanDomain.isBlank()) {
                return ValidationResult.error("빈 도메인은 등록할 수 없습니다.")
            }

            // 금지된 패턴 확인
            if (isForbiddenPattern(cleanDomain)) {
                return ValidationResult.error("허용되지 않는 도메인 패턴입니다: $cleanDomain")
            }

            // 도메인 형식 검증
            if (!isValidDomainFormat(cleanDomain)) {
                return ValidationResult.error("올바르지 않은 도메인 형식입니다: $cleanDomain")
            }

            // localhost/IP 제한 (프로덕션에서)
            if (isLocalOrIpAddress(cleanDomain)) {
                return ValidationResult.error("로컬 도메인이나 IP 주소는 등록할 수 없습니다: $cleanDomain")
            }
        }

        return ValidationResult.success("모든 도메인이 유효합니다.")
    }

    /**
     * 사이트 소유권 검증 (선택적 - 향후 구현)
     */
    fun verifySiteOwnership(domain: String, siteKey: String): Boolean {
        // TODO: DNS TXT 레코드나 파일 업로드를 통한 소유권 검증
        // 예: TXT 레코드에 "comdeply-verification=${siteKey}" 확인
        return true // 임시로 항상 true 반환
    }

    private fun cleanDomain(domain: String): String {
        return domain.trim()
            .lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .split("/")[0] // 경로 제거
            .split(":")[0] // 포트 제거
    }

    private fun isForbiddenPattern(domain: String): Boolean {
        return FORBIDDEN_PATTERNS.any { pattern ->
            domain.contains(pattern, ignoreCase = true)
        }
    }

    private fun isValidDomainFormat(domain: String): Boolean {
        // 기본 도메인 패턴 검사
        if (!DOMAIN_PATTERN.matcher(domain).matches()) {
            return false
        }

        // 추가 검증
        try {
            URI("http://$domain")
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun isLocalOrIpAddress(domain: String): Boolean {
        return when {
            domain == "localhost" -> true
            domain.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) -> true // IP 주소
            domain.endsWith(".local") -> true
            domain.endsWith(".localhost") -> true
            else -> false
        }
    }
}
