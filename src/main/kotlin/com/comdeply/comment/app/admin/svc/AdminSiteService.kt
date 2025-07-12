package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.app.admin.svc.vo.SiteDeletionResponse
import com.comdeply.comment.app.admin.svc.vo.SiteStatsResponse
import com.comdeply.comment.app.admin.svc.vo.SiteStatusResponse
import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.Site
import com.comdeply.comment.entity.SitePage
import com.comdeply.comment.entity.PageType
import com.comdeply.comment.entity.SitePermission
import com.comdeply.comment.repository.CommentRepository
import com.comdeply.comment.repository.SiteRepository
import com.comdeply.comment.repository.SitePageRepository
import com.comdeply.comment.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class AdminSiteService(
    private val siteRepository: SiteRepository,
    private val sitePageRepository: SitePageRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val adminPermissionService: AdminPermissionService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(AdminSiteService::class.java)

    /**
     * 관리자 권한에 따른 사이트 목록 조회
     */
    @Transactional(readOnly = true)
    fun getSitesByPermission(admin: Admin, page: Int, size: Int): Page<SiteResponse> {
        logger.info("관리자 권한에 따른 사이트 목록 조회: adminId={}, page={}, size={}", admin.id, page, size)

        val pageable = PageRequest.of(page, size)
        val accessibleSiteIds = adminPermissionService.getAccessibleSiteIds(admin)
        val isGlobalAccess = adminPermissionService.canViewGlobalStats(admin)

        val sitesPage = if (isGlobalAccess) {
            // 수퍼관리자: 모든 사이트 조회
            siteRepository.findAll(pageable)
        } else {
            // 일반 관리자: 접근 가능한 사이트만 조회
            if (accessibleSiteIds.isEmpty()) {
                return Page.empty(pageable)
            }
            siteRepository.findAllById(accessibleSiteIds).let { sites ->
                val startIndex = page * size
                val endIndex = minOf(startIndex + size, sites.size)
                val content = if (startIndex < sites.size) sites.subList(startIndex, endIndex) else emptyList()

                org.springframework.data.domain.PageImpl(content, pageable, sites.size.toLong())
            }
        }

        val responsePages = sitesPage.map { site -> convertToSiteResponse(site) }

        logger.info("사이트 목록 조회 완료: 총 {}개", responsePages.totalElements)
        return responsePages
    }

    /**
     * 새로운 사이트 생성
     */
    fun createSite(request: SiteCreateRequest, admin: Admin): SiteResponse {
        logger.info("새로운 사이트 생성: adminId={}, domain={}", admin.id, request.domain)

        // 사이트 생성 권한 확인 (SUPER_ADMIN만 가능)
        if (!adminPermissionService.canViewGlobalStats(admin)) {
            throw IllegalArgumentException("사이트 생성 권한이 없습니다")
        }

        try {
            // 도메인 중복 확인
            if (siteRepository.findByDomain(request.domain) != null) {
                throw IllegalArgumentException("이미 존재하는 도메인입니다")
            }

            // 사이트 키 생성
            val siteKey = UUID.randomUUID().toString()

            // 허용 도메인 목록 JSON 변환
            val allowedDomainsJson = request.allowedDomains?.let { domains ->
                objectMapper.writeValueAsString(domains)
            }

            val site = Site(
                ownerId = admin.id,
                siteName = request.siteName,
                domain = request.domain,
                siteKey = siteKey,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                isActive = true,
                themeColor = request.themeColor,
                customCss = request.customCss,
                requireAuth = request.requireAuth,
                enableModeration = request.enableModeration,
                theme = request.theme,
                language = request.language,
                allowedDomains = allowedDomainsJson
            )

            val savedSite = siteRepository.save(site)
            val response = convertToSiteResponse(savedSite)

            logger.info("사이트 생성 완료: siteId={}, domain={}", savedSite.id, savedSite.domain)
            return response
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 생성 실패: domain={}, error={}", request.domain, e.message)
            throw e
        } catch (e: Exception) {
            logger.error("사이트 생성 중 예상치 못한 오류 발생: domain={}", request.domain, e)
            throw e
        }
    }

    /**
     * 사이트 상세 정보 조회
     */
    @Transactional(readOnly = true)
    fun getSiteDetail(siteId: Long, admin: Admin): SiteResponse {
        logger.info("사이트 상세 정보 조회: siteId={}, adminId={}", siteId, admin.id)

        // 사이트 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        try {
            val site = siteRepository.findById(siteId).orElse(null)
                ?: throw IllegalArgumentException("사이트를 찾을 수 없습니다")

            val response = convertToSiteResponse(site)
            logger.info("사이트 상세 정보 조회 완료: siteId={}, domain={}", siteId, site.domain)
            return response
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 상세 정보 조회 실패: siteId={}, error={}", siteId, e.message)
            throw e
        } catch (e: Exception) {
            logger.error("사이트 상세 정보 조회 중 예상치 못한 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * 사이트 정보 수정
     */
    fun updateSite(siteId: Long, request: SiteUpdateRequest, admin: Admin): SiteResponse {
        logger.info("사이트 정보 수정: siteId={}, adminId={}", siteId, admin.id)

        // 사이트 수정 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        // 사이트 수정 권한 확인 (MANAGE 권한 필요)
        if (!adminPermissionService.hasPermissionForSite(admin, siteId, SitePermission.MANAGE)) {
            throw IllegalArgumentException("사이트 수정 권한이 없습니다")
        }

        try {
            val existingSite = siteRepository.findById(siteId).orElse(null)
                ?: throw IllegalArgumentException("사이트를 찾을 수 없습니다")

            // 도메인 변경 시 중복 확인
            if (request.domain != null && request.domain != existingSite.domain) {
                if (siteRepository.findByDomain(request.domain) != null) {
                    throw IllegalArgumentException("이미 존재하는 도메인입니다")
                }
            }

            // 허용 도메인 목록 JSON 변환
            val allowedDomainsJson = request.allowedDomains?.let { domains ->
                objectMapper.writeValueAsString(domains)
            }

            val updatedSite = existingSite.copy(
                siteName = request.siteName ?: existingSite.siteName,
                domain = request.domain ?: existingSite.domain,
                themeColor = request.themeColor ?: existingSite.themeColor,
                customCss = request.customCss ?: existingSite.customCss,
                requireAuth = request.requireAuth ?: existingSite.requireAuth,
                enableModeration = request.enableModeration ?: existingSite.enableModeration,
                theme = request.theme ?: existingSite.theme,
                language = request.language ?: existingSite.language,
                isActive = request.isActive ?: existingSite.isActive,
                allowedDomains = allowedDomainsJson ?: existingSite.allowedDomains,
                updatedAt = LocalDateTime.now()
            )

            val savedSite = siteRepository.save(updatedSite)
            val response = convertToSiteResponse(savedSite)

            logger.info("사이트 정보 수정 완료: siteId={}, domain={}", siteId, savedSite.domain)
            return response
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 정보 수정 실패: siteId={}, error={}", siteId, e.message)
            throw e
        } catch (e: Exception) {
            logger.error("사이트 정보 수정 중 예상치 못한 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * 사이트 삭제 (비활성화)
     */
    fun deleteSite(siteId: Long, admin: Admin): SiteDeletionResponse {
        logger.info("사이트 삭제 요청: siteId={}, adminId={}", siteId, admin.id)

        // 사이트 삭제 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        // 사이트 삭제 권한 확인 (SUPER_ADMIN만 가능)
        if (!adminPermissionService.canViewGlobalStats(admin)) {
            throw IllegalArgumentException("사이트 삭제 권한이 없습니다")
        }

        try {
            val existingSite = siteRepository.findById(siteId).orElse(null)
                ?: throw IllegalArgumentException("사이트를 찾을 수 없습니다")

            // 사이트를 비활성화 (실제 삭제하지 않음)
            val deletedSite = existingSite.copy(
                isActive = false,
                updatedAt = LocalDateTime.now()
            )

            siteRepository.save(deletedSite)

            val response = SiteDeletionResponse(
                siteId = siteId,
                message = "사이트가 성공적으로 삭제되었습니다",
                deletedAt = System.currentTimeMillis()
            )

            logger.info("사이트 삭제 완료: siteId={}", siteId)
            return response
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 삭제 실패: siteId={}, error={}", siteId, e.message)
            throw e
        } catch (e: Exception) {
            logger.error("사이트 삭제 중 예상치 못한 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * 사이트 통계 조회
     */
    @Transactional(readOnly = true)
    fun getSiteStats(siteId: Long, admin: Admin): SiteStatsResponse {
        logger.info("사이트 통계 조회: siteId={}, adminId={}", siteId, admin.id)

        // 사이트 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        try {
            // 사이트 존재 여부 확인
            val site = siteRepository.findById(siteId).orElse(null)
                ?: throw IllegalArgumentException("사이트를 찾을 수 없습니다")

            // 댓글 통계 조회
            val totalComments = commentRepository.countBySiteId(siteId)
            val pendingComments = commentRepository.countBySiteIdInAndStatus(listOf(siteId), com.comdeply.comment.entity.CommentStatus.PENDING)
            val approvedComments = commentRepository.countBySiteIdInAndStatus(listOf(siteId), com.comdeply.comment.entity.CommentStatus.APPROVED)
            val rejectedComments = commentRepository.countBySiteIdInAndStatus(listOf(siteId), com.comdeply.comment.entity.CommentStatus.REJECTED)

            // 사용자 통계 조회
            val totalUsers = userRepository.countBySiteIdIn(listOf(siteId))
            val activeUsers = totalUsers // 임시로 전체 사용자 수와 동일하게 처리 (isActive 필드 확인 필요)

            // 페이지 통계 (고유 페이지 수 계산) - 간단한 방법으로 대체
            val siteComments = commentRepository.findBySiteId(siteId, org.springframework.data.domain.PageRequest.of(0, Int.MAX_VALUE))
            val totalPages = siteComments.content.map { it.pageId }.distinct().size.toLong()
            val activePages = totalPages // 임시로 전체 페이지 수와 동일하게 처리

            val response = SiteStatsResponse(
                siteId = siteId,
                totalComments = totalComments,
                pendingComments = pendingComments,
                approvedComments = approvedComments,
                rejectedComments = rejectedComments,
                totalPages = totalPages,
                activePages = activePages,
                totalUsers = totalUsers,
                activeUsers = activeUsers,
                lastUpdated = System.currentTimeMillis()
            )

            logger.info("사이트 통계 조회 완료: siteId={}, totalComments={}", siteId, response.totalComments)
            return response
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 통계 조회 실패: siteId={}, error={}", siteId, e.message)
            throw e
        } catch (e: Exception) {
            logger.error("사이트 통계 조회 중 예상치 못한 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * 사이트별 페이지 목록 조회
     */
    @Transactional(readOnly = true)
    fun getSitePages(siteId: Long, admin: Admin, page: Int, size: Int): Page<PageStatsResponse> {
        logger.info(
            "사이트 페이지 목록 조회: siteId={}, adminId={}, page={}, size={}",
            siteId,
            admin.id,
            page,
            size
        )

        // 사이트 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        try {
            // 사이트 존재 여부 확인
            val site = siteRepository.findById(siteId).orElse(null)
                ?: throw IllegalArgumentException("사이트를 찾을 수 없습니다")

            // 사이트의 페이지 목록 조회 (pageId 기준으로 그룹핑)
            val pageable = PageRequest.of(page, size)
            val comments = commentRepository.findBySiteId(siteId, org.springframework.data.domain.PageRequest.of(0, Int.MAX_VALUE)).content.sortedByDescending { it.createdAt }

            // 페이지별 통계 생성
            val pageStats = comments.groupBy { it.pageId }
                .map { (pageId, pageComments) ->
                    PageStatsResponse(
                        pageId = pageId,
                        commentCount = pageComments.size.toLong(),
                        lastCommentAt = pageComments.maxByOrNull { it.createdAt }?.createdAt
                    )
                }
                .sortedByDescending { it.lastCommentAt }

            // 페이징 적용
            val startIndex = page * size
            val endIndex = minOf(startIndex + size, pageStats.size)
            val content = if (startIndex < pageStats.size) pageStats.subList(startIndex, endIndex) else emptyList()

            val pagesPage = org.springframework.data.domain.PageImpl(content, pageable, pageStats.size.toLong())

            logger.info("사이트 페이지 목록 조회 완료: siteId={}, 총 {}개", siteId, pagesPage.totalElements)
            return pagesPage
        } catch (e: Exception) {
            logger.error("사이트 페이지 목록 조회 중 예상치 못한 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * 사이트 활성화/비활성화
     */
    fun toggleSiteStatus(siteId: Long, admin: Admin): SiteStatusResponse {
        logger.info("사이트 상태 토글: siteId={}, adminId={}", siteId, admin.id)

        // 사이트 상태 변경 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        if (!adminPermissionService.hasPermissionForSite(admin, siteId, SitePermission.MANAGE)) {
            throw IllegalArgumentException("사이트 상태 변경 권한이 없습니다")
        }

        try {
            val currentSite = siteRepository.findById(siteId).orElse(null)
                ?: throw IllegalArgumentException("사이트를 찾을 수 없습니다")

            val newStatus = !currentSite.isActive

            val updatedSite = currentSite.copy(
                isActive = newStatus,
                updatedAt = LocalDateTime.now()
            )

            siteRepository.save(updatedSite)

            val response = SiteStatusResponse(
                siteId = siteId,
                isActive = newStatus,
                message = if (newStatus) "사이트가 활성화되었습니다" else "사이트가 비활성화되었습니다",
                changedAt = System.currentTimeMillis()
            )

            logger.info("사이트 상태 변경 완료: siteId={}, isActive={}", siteId, newStatus)
            return response
        } catch (e: IllegalArgumentException) {
            logger.warn("사이트 상태 변경 실패: siteId={}, error={}", siteId, e.message)
            throw e
        } catch (e: Exception) {
            logger.error("사이트 상태 변경 중 예상치 못한 오류 발생: siteId={}", siteId, e)
            throw e
        }
    }

    /**
     * Site 엔티티를 SiteResponse DTO로 변환
     */
    private fun convertToSiteResponse(site: Site): SiteResponse {
        // 허용된 도메인 목록 JSON 파싱
        val allowedDomains = site.allowedDomains?.let { domainsJson ->
            try {
                objectMapper.readValue(domainsJson, Array<String>::class.java).toList()
            } catch (e: Exception) {
                logger.warn("허용된 도메인 목록 파싱 실패: {}", e.message)
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
            embedCode = generateEmbedCode(site),
            allowedDomains = allowedDomains
        )
    }

    /**
     * 페이지 생성
     */
    fun createPage(siteId: Long, request: PageCreateRequest, admin: Admin): SitePageResponse {
        logger.info("페이지 생성 요청: siteId={}, pageId={}, adminId={}", siteId, request.pageId, admin.id)

        // 사이트 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        // 사이트 존재 확인
        val site = siteRepository.findById(siteId).orElse(null)
            ?: throw IllegalArgumentException("사이트를 찾을 수 없습니다")

        // 중복 페이지ID 확인
        val existingPage = sitePageRepository.findBySiteIdAndPageIdAndIsActiveTrue(site.siteKey, request.pageId)
        if (existingPage != null) {
            throw IllegalArgumentException("이미 존재하는 페이지 ID입니다")
        }

        try {
            val pageType = try {
                PageType.valueOf(request.pageType.uppercase())
            } catch (e: IllegalArgumentException) {
                PageType.GENERAL
            }

            val newPage = SitePage(
                siteId = site.siteKey,
                pageId = request.pageId,
                pageName = request.pageName,
                pageDescription = request.pageDescription,
                pageType = pageType,
                pageUrl = request.pageUrl,
                commentCount = 0,
                lastActivityAt = LocalDateTime.now()
            )

            val savedPage = sitePageRepository.save(newPage)

            val response = SitePageResponse(
                id = savedPage.id,
                siteId = savedPage.siteId,
                pageId = savedPage.pageId,
                pageName = savedPage.pageName,
                pageDescription = savedPage.pageDescription,
                pageType = savedPage.pageType.name,
                pageUrl = savedPage.pageUrl,
                commentCount = savedPage.commentCount,
                lastActivityAt = savedPage.lastActivityAt,
                createdAt = savedPage.createdAt,
                updatedAt = savedPage.updatedAt,
                isActive = savedPage.isActive
            )

            logger.info("페이지 생성 완료: siteId={}, pageId={}", siteId, request.pageId)
            return response
        } catch (e: Exception) {
            logger.error("페이지 생성 중 오류 발생: siteId={}, pageId={}", siteId, request.pageId, e)
            throw e
        }
    }

    /**
     * 페이지 상세 조회
     */
    @Transactional(readOnly = true)
    fun getPageDetail(siteId: Long, pageId: String, admin: Admin): SitePageResponse {
        logger.info("페이지 상세 조회 요청: siteId={}, pageId={}, adminId={}", siteId, pageId, admin.id)

        // 사이트 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        // 사이트 존재 확인
        val site = siteRepository.findById(siteId).orElse(null)
            ?: throw IllegalArgumentException("사이트를 찾을 수 없습니다")

        val page = sitePageRepository.findBySiteIdAndPageIdAndIsActiveTrue(site.siteKey, pageId)
            ?: throw IllegalArgumentException("페이지를 찾을 수 없습니다")

        return SitePageResponse(
            id = page.id,
            siteId = page.siteId,
            pageId = page.pageId,
            pageName = page.pageName,
            pageDescription = page.pageDescription,
            pageType = page.pageType.name,
            pageUrl = page.pageUrl,
            commentCount = page.commentCount,
            lastActivityAt = page.lastActivityAt,
            createdAt = page.createdAt,
            updatedAt = page.updatedAt,
            isActive = page.isActive
        )
    }

    /**
     * 페이지 수정
     */
    fun updatePage(siteId: Long, pageId: String, request: PageUpdateRequest, admin: Admin): SitePageResponse {
        logger.info("페이지 수정 요청: siteId={}, pageId={}, adminId={}", siteId, pageId, admin.id)

        // 사이트 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        // 사이트 존재 확인
        val site = siteRepository.findById(siteId).orElse(null)
            ?: throw IllegalArgumentException("사이트를 찾을 수 없습니다")

        val existingPage = sitePageRepository.findBySiteIdAndPageIdAndIsActiveTrue(site.siteKey, pageId)
            ?: throw IllegalArgumentException("페이지를 찾을 수 없습니다")

        try {
            val pageType = request.pageType?.let {
                try {
                    PageType.valueOf(it.uppercase())
                } catch (e: IllegalArgumentException) {
                    existingPage.pageType
                }
            } ?: existingPage.pageType

            val updatedPage = existingPage.copy(
                pageName = request.pageName ?: existingPage.pageName,
                pageDescription = request.pageDescription ?: existingPage.pageDescription,
                pageType = pageType,
                pageUrl = request.pageUrl ?: existingPage.pageUrl,
                isActive = request.isActive ?: existingPage.isActive,
                updatedAt = LocalDateTime.now()
            )

            val savedPage = sitePageRepository.save(updatedPage)

            val response = SitePageResponse(
                id = savedPage.id,
                siteId = savedPage.siteId,
                pageId = savedPage.pageId,
                pageName = savedPage.pageName,
                pageDescription = savedPage.pageDescription,
                pageType = savedPage.pageType.name,
                pageUrl = savedPage.pageUrl,
                commentCount = savedPage.commentCount,
                lastActivityAt = savedPage.lastActivityAt,
                createdAt = savedPage.createdAt,
                updatedAt = savedPage.updatedAt,
                isActive = savedPage.isActive
            )

            logger.info("페이지 수정 완료: siteId={}, pageId={}", siteId, pageId)
            return response
        } catch (e: Exception) {
            logger.error("페이지 수정 중 오류 발생: siteId={}, pageId={}", siteId, pageId, e)
            throw e
        }
    }

    /**
     * 페이지 삭제 (비활성화)
     */
    fun deletePage(siteId: Long, pageId: String, admin: Admin) {
        logger.info("페이지 삭제 요청: siteId={}, pageId={}, adminId={}", siteId, pageId, admin.id)

        // 사이트 접근 권한 확인
        if (!adminPermissionService.hasPermissionForSite(admin, siteId)) {
            throw IllegalArgumentException("해당 사이트에 대한 접근 권한이 없습니다")
        }

        // 사이트 존재 확인
        val site = siteRepository.findById(siteId).orElse(null)
            ?: throw IllegalArgumentException("사이트를 찾을 수 없습니다")

        val existingPage = sitePageRepository.findBySiteIdAndPageIdAndIsActiveTrue(site.siteKey, pageId)
            ?: throw IllegalArgumentException("페이지를 찾을 수 없습니다")

        try {
            // 페이지를 비활성화 (실제 삭제하지 않음)
            val deletedPage = existingPage.copy(
                isActive = false,
                updatedAt = LocalDateTime.now()
            )

            sitePageRepository.save(deletedPage)

            logger.info("페이지 삭제 완료: siteId={}, pageId={}", siteId, pageId)
        } catch (e: Exception) {
            logger.error("페이지 삭제 중 오류 발생: siteId={}, pageId={}", siteId, pageId, e)
            throw e
        }
    }

    /**
     * 사이트 임베드 코드 생성
     */
    private fun generateEmbedCode(site: Site): String {
        return """
            <script>
                window.ComDeplyConfig = {
                    siteKey: '${site.siteKey}',
                    theme: '${site.theme}',
                    themeColor: '${site.themeColor}',
                    language: '${site.language}',
                    requireAuth: ${site.requireAuth}
                };
            </script>
            <script src="https://cdn.comdeply.com/embed.js"></script>
        """.trimIndent()
    }
}
