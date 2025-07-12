package com.comdeply.comment.app.web.svc

import com.comdeply.comment.app.admin.svc.vo.SiteStatsResponse2
import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.Site
import com.comdeply.comment.repository.CommentRepository
import com.comdeply.comment.repository.SiteRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class SiteService(
    private val siteRepository: SiteRepository,
    private val commentRepository: CommentRepository,
    private val objectMapper: ObjectMapper,
    private val domainValidationService: DomainValidationService,
    private val planValidationService: PlanValidationService
) {

    /**
     * 최초 가입시 사이트 생성 (각종 체크 생략)
     */
    fun createInitialSite(request: SiteCreateRequest, ownerId: Long): SiteResponse {
        // 최초 가입시 사이트 생성 - 각종 체크 생략
        val siteKey = UUID.randomUUID().toString().take(12)

        val site = Site(
            siteName = request.siteName,
            siteKey = siteKey,
            domain = request.domain,
            ownerId = ownerId,
            themeColor = request.themeColor ?: "#007bff", // 기본값 설정
            customCss = request.customCss,
            requireAuth = request.requireAuth ?: false, // 기본값 설정
            enableModeration = request.enableModeration ?: false, // 기본값 설정
            theme = request.theme ?: "default", // 기본값 설정
            language = request.language ?: "ko", // 기본값 설정
            allowedDomains = request.allowedDomains?.let { objectMapper.writeValueAsString(it) }
        )

        val savedSite = siteRepository.save(site)
        return toSiteResponse(savedSite)
    }

    /**
     * 새 사이트 생성 (플랜 제한 및 도메인 검증 포함)
     */
    fun createSite(request: SiteCreateRequest, ownerId: Long): SiteResponse {
        // 사이트 생성 가능 여부 확인 (플랜 제한)
        val siteValidation = planValidationService.canCreateSite(ownerId)
        if (!siteValidation.isValid) {
            throw IllegalArgumentException(siteValidation.message)
        }

        // 사이트 이름 중복 확인
        val existingSite = siteRepository.findBySiteNameAndOwnerId(request.siteName, ownerId)
        if (existingSite != null) {
            throw IllegalArgumentException("이미 존재하는 사이트 이름입니다.")
        }

        // 허용 도메인 검증 (플랜별 제한 적용)
        request.allowedDomains?.let { domains ->
            val validationResult = domainValidationService.validateDomainList(domains, ownerId, 0)
            if (!validationResult.isValid) {
                throw IllegalArgumentException(validationResult.message)
            }
        }

        // site_id 는 유일한 키를 자동생성한다. 12자리 자송생성(UUID 등)
        // 유일한키 12자리로 생성
        val siteKey = UUID.randomUUID().toString().take(12)

        val site = Site(
            siteName = request.siteName,
            siteKey = siteKey,
            domain = request.domain,
            ownerId = ownerId,
            themeColor = request.themeColor,
            customCss = request.customCss,
            requireAuth = request.requireAuth,
            enableModeration = request.enableModeration,
            theme = request.theme,
            language = request.language,
            allowedDomains = request.allowedDomains?.let { objectMapper.writeValueAsString(it) }
        )

        val savedSite = siteRepository.save(site)
        return toSiteResponse(savedSite)
    }

    /**
     * 사이트 정보 수정
     */
    fun updateSite(siteId: Long, request: SiteUpdateRequest, ownerId: Long): SiteResponse {
        val site = siteRepository.findById(siteId).orElseThrow { IllegalArgumentException("사이트 정보가 존재하지 않습니다.") }

        if (site.ownerId != ownerId) {
            throw IllegalArgumentException("자신의 사이트만 수정할 수 있습니다.")
        }

        // 허용 도메인 검증 (기존 도메인 수 포함)
        request.allowedDomains?.let { domains ->
            val currentDomainCount = site.allowedDomains?.let {
                try {
                    objectMapper.readValue(it, List::class.java).size
                } catch (e: Exception) {
                    0
                }
            } ?: 0

            val validationResult = domainValidationService.validateDomainList(domains, ownerId, currentDomainCount)
            if (!validationResult.isValid) {
                throw IllegalArgumentException(validationResult.message)
            }
        }

        val updatedSite = site.copy(
            siteName = request.siteName ?: site.siteName,
            domain = request.domain ?: site.domain,
            themeColor = request.themeColor ?: site.themeColor,
            customCss = request.customCss ?: site.customCss,
            requireAuth = request.requireAuth ?: site.requireAuth,
            enableModeration = request.enableModeration ?: site.enableModeration,
            theme = request.theme ?: site.theme,
            language = request.language ?: site.language,
            isActive = request.isActive ?: site.isActive,
            allowedDomains = request.allowedDomains?.let { objectMapper.writeValueAsString(it) } ?: site.allowedDomains,
            updatedAt = LocalDateTime.now()
        )

        val savedSite = siteRepository.save(updatedSite)
        return toSiteResponse(savedSite)
    }

    /**
     * 사이트 정보 조회
     */
    fun getSite(siteId: Long): SiteResponse {
        val site = siteRepository.findById(siteId).orElseThrow { IllegalArgumentException("Site가 존재하지 않습니다.") }
        return toSiteResponse(site)
    }

    /**
     * 사용자가 소유한 사이트 목록 조회
     */
    fun getUserSites(ownerId: Long): SiteListResponse {
        val sites = siteRepository.findByOwnerId(ownerId)
        val siteResponses = sites.map { toSiteResponse(it) }

        return SiteListResponse(
            sites = siteResponses,
            totalCount = sites.size.toLong()
        )
    }

    /**
     * 활성 상태인 모든 사이트 목록 조회
     */
    fun getActiveSites(): SiteListResponse {
        val sites = siteRepository.findByIsActiveTrue()
        val siteResponses = sites.map { toSiteResponse(it) }

        return SiteListResponse(
            sites = siteResponses,
            totalCount = sites.size.toLong()
        )
    }

    /**
     * 사이트 키로 사이트 찾기
     */
    fun findBySiteKey(shortName: String): SiteResponse? {
        val site = siteRepository.findBySiteKey(shortName) ?: return null
        return toSiteResponse(site)
    }

    /**
     * 사이트 삭제 (비활성화 처리)
     */
    fun deleteSite(siteId: Long, ownerId: Long) {
        val site = siteRepository.findById(siteId).orElseThrow { IllegalArgumentException("Site not found") }

        if (site.ownerId != ownerId) {
            throw IllegalArgumentException("You can only delete your own sites")
        }

        // 사이트를 비활성화하는 것으로 처리 (댓글 데이터 보존)
        val deactivatedSite = site.copy(
            isActive = false,
            updatedAt = LocalDateTime.now()
        )

        siteRepository.save(deactivatedSite)
    }

    /**
     * 사이트 통계 정보 조회
     */
    fun getSiteStats(siteId: Long, ownerId: Long): SiteStatsResponse2 {
        val site = siteRepository.findById(siteId).orElseThrow { IllegalArgumentException("Site not found") }

        if (site.ownerId != ownerId) {
            throw IllegalArgumentException("You can only view stats for your own sites")
        }

        // 기본 통계 (실제 구현에서는 더 복잡한 쿼리가 필요)
        val totalComments = commentRepository.countBySiteKeyAndPageIdAndIsDeletedFalse(site.siteKey, "")

        return SiteStatsResponse2(
            siteId = siteId,
            totalComments = totalComments,
            totalUsers = 0, // TODO: 사용자 통계 구현
            todayComments = 0, // TODO: 오늘 댓글 통계 구현
            popularPages = emptyList() // TODO: 인기 페이지 통계 구현
        )
    }

    /**
     * Site 엔티티를 SiteResponse DTO로 변환
     */
    private fun toSiteResponse(site: Site): SiteResponse {
        val embedCode = generateEmbedCode(site.siteKey)
        val allowedDomains = site.allowedDomains?.let {
            try {
                objectMapper.readValue(it, List::class.java) as List<String>
            } catch (e: Exception) {
                null
            }
        }

        return SiteResponse(
            id = site.id,
            siteName = site.siteName,
            domain = site.domain,
            ownerId = site.ownerId,
            siteKey = site.siteKey,
            createdAt = site.createdAt,
            updatedAt = site.updatedAt,
            isActive = site.isActive,
            themeColor = site.themeColor,
            customCss = site.customCss,
            requireAuth = site.requireAuth,
            enableModeration = site.enableModeration,
            theme = site.theme,
            language = site.language,
            embedCode = embedCode,
            allowedDomains = allowedDomains
        )
    }

    // === Admin-specific methods ===

    /**
     * 관리자가 사이트 생성
     */
    fun createSiteByAdmin(request: SiteCreateRequest, adminId: Long): SiteResponse {
        val siteKey = UUID.randomUUID().toString().take(12)

        val site = Site(
            siteName = request.siteName,
            siteKey = siteKey,
            domain = request.domain,
            ownerId = adminId, // Admin becomes the owner
            themeColor = request.themeColor ?: "#007bff",
            customCss = request.customCss,
            requireAuth = request.requireAuth ?: false,
            enableModeration = request.enableModeration ?: true,
            theme = request.theme ?: "light",
            language = request.language ?: "ko",
            allowedDomains = request.allowedDomains?.let { objectMapper.writeValueAsString(it) }
        )

        val savedSite = siteRepository.save(site)
        return toSiteResponse(savedSite)
    }

    /**
     * 관리자가 사이트 정보 수정
     */
    fun updateSiteByAdmin(siteId: Long, request: SiteUpdateRequest): SiteResponse {
        val site = siteRepository.findById(siteId)
            .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다: $siteId") }

        val updatedSite = site.copy(
            siteName = request.siteName ?: site.siteName,
            domain = request.domain ?: site.domain,
            themeColor = request.themeColor ?: site.themeColor,
            customCss = request.customCss ?: site.customCss,
            requireAuth = request.requireAuth ?: site.requireAuth,
            enableModeration = request.enableModeration ?: site.enableModeration,
            theme = request.theme ?: site.theme,
            language = request.language ?: site.language,
            isActive = request.isActive ?: site.isActive,
            allowedDomains = request.allowedDomains?.let { objectMapper.writeValueAsString(it) } ?: site.allowedDomains,
            updatedAt = LocalDateTime.now()
        )

        val savedSite = siteRepository.save(updatedSite)
        return toSiteResponse(savedSite)
    }

    /**
     * 관리자가 사이트 삭제
     */
    fun deleteSiteByAdmin(siteId: Long) {
        val site = siteRepository.findById(siteId)
            .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다: $siteId") }

        // 사이트를 비활성화하는 것으로 처리 (댓글 데이터 보존)
        val deactivatedSite = site.copy(
            isActive = false,
            updatedAt = LocalDateTime.now()
        )

        siteRepository.save(deactivatedSite)
    }

    /**
     * 관리자가 사이트 정보 조회
     */
    fun getSiteByAdmin(siteId: Long): SiteResponse {
        val site = siteRepository.findById(siteId)
            .orElseThrow { IllegalArgumentException("사이트를 찾을 수 없습니다: $siteId") }
        return toSiteResponse(site)
    }

    /**
     * 사이트 임베드 코드 생성
     */
    private fun generateEmbedCode(siteKey: String): String {
        return """
            <script src="https://comment.comdeply.com/embed.js" 
                    data-site="$siteKey" 
                    data-page-id="{{PAGE_ID}}">
            </script>
            <div id="comdeply-comments"></div>
        """.trimIndent()
    }
}
