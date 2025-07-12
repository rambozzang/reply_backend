package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.config.JwtTokenProvider
import com.comdeply.comment.dto.SkinDto
import com.comdeply.comment.entity.*
import com.comdeply.comment.repository.AdminRepository
import com.comdeply.comment.repository.SkinRepository
import com.comdeply.comment.repository.SkinApplicationRepository
import com.comdeply.comment.repository.AdminSitePermissionRepository
import com.comdeply.comment.repository.SitePageRepository
import com.comdeply.comment.repository.SiteRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime

@Service
@Transactional
class AdminSkinService(
    private val skinRepository: SkinRepository,
    private val adminRepository: AdminRepository,
    private val skinApplicationRepository: SkinApplicationRepository,
    private val adminSitePermissionRepository: AdminSitePermissionRepository,
    private val sitePageRepository: SitePageRepository,
    private val siteRepository: SiteRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper
) {
    
    private val skinsDirectory = "/Users/bumkyuchun/work/app/comdeply/admin/public/kit/comment-widget-skins"
    
    /**
     * 관리자 ID 추출
     */
    private fun extractAdminIdFromToken(token: String): Long {
        val actualToken = token.replace("Bearer ", "")
        return jwtTokenProvider.getAdminIdFromToken(actualToken)
    }
    
    /**
     * 스킨 목록 조회
     */
    fun getSkins(token: String): List<SkinDto.SkinResponse> {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("관리자를 찾을 수 없습니다.") }
        
        val skins = if (admin.role == AdminRole.SUPER_ADMIN) {
            // 슈퍼 관리자: 모든 스킨 조회
            skinRepository.findAll()
        } else {
            // 일반 관리자: 공용 스킨 + 자신이 만든 스킨
            skinRepository.findByIsSharedTrueOrAdminId(adminId)
        }
        
        // DB에 스킨이 없으면 파일 시스템에서 동기화
        if (skins.isEmpty()) {
            syncSkinsFromFileSystem()
            return getSkins(token)
        }
        
        return skins.map { skin ->
            SkinDto.SkinResponse(
                id = skin.id,
                name = skin.name,
                description = skin.description,
                type = skin.type.name,
                adminId = skin.adminId,
                isShared = skin.isShared,
                isOwner = skin.adminId == adminId,
                theme = parseTheme(skin.theme),
                styles = skin.styles ?: "",
                createdAt = skin.createdAt.toString(),
                updatedAt = skin.updatedAt.toString()
            )
        }
    }
    
    /**
     * 스킨 생성
     */
    fun createSkin(token: String, request: SkinDto.CreateSkinRequest): SkinDto.SkinResponse {
        val adminId = extractAdminIdFromToken(token)
        
        // 동일한 이름의 스킨이 있는지 확인
        if (skinRepository.existsByName(request.name)) {
            throw IllegalArgumentException("이미 존재하는 스킨 이름입니다.")
        }
        
        val skin = Skin(
            name = request.name,
            description = request.description,
            type = SkinType.PRIVATE,
            adminId = adminId,
            isShared = false,
            theme = objectMapper.writeValueAsString(request.theme),
            styles = request.styles,
            folderName = generateSkinFileName(request.name)
        )
        
        val savedSkin = skinRepository.save(skin)
        
        // 파일 시스템에 스킨 파일 생성
        createSkinFiles(savedSkin)
        
        return SkinDto.SkinResponse(
            id = savedSkin.id,
            name = savedSkin.name,
            description = savedSkin.description,
            type = savedSkin.type.name,
            adminId = savedSkin.adminId,
            isShared = savedSkin.isShared,
            isOwner = true,
            theme = request.theme,
            styles = savedSkin.styles ?: "",
            createdAt = savedSkin.createdAt.toString(),
            updatedAt = savedSkin.updatedAt.toString()
        )
    }
    
    /**
     * 스킨 수정
     */
    fun updateSkin(token: String, skinId: Long, request: SkinDto.UpdateSkinRequest): SkinDto.SkinResponse {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("관리자를 찾을 수 없습니다.") }
        
        val skin = skinRepository.findById(skinId)
            .orElseThrow { IllegalArgumentException("스킨을 찾을 수 없습니다.") }
        
        // 권한 체크: 슈퍼 관리자이거나 스킨 소유자만 수정 가능
        if (admin.role != AdminRole.SUPER_ADMIN && skin.adminId != adminId) {
            throw IllegalAccessException("스킨을 수정할 권한이 없습니다.")
        }
        
        // 시스템 스킨은 수정 불가
        if (skin.type == SkinType.BUILT_IN) {
            throw IllegalArgumentException("시스템 스킨은 수정할 수 없습니다.")
        }
        
        // 업데이트
        request.name?.let { 
            if (it != skin.name && skinRepository.existsByName(it)) {
                throw IllegalArgumentException("이미 존재하는 스킨 이름입니다.")
            }
            skin.name = it 
        }
        request.description?.let { skin.description = it }
        request.theme?.let { skin.theme = objectMapper.writeValueAsString(it) }
        request.styles?.let { skin.styles = it }
        skin.updatedAt = LocalDateTime.now()
        
        val updatedSkin = skinRepository.save(skin)
        
        // 파일 시스템 업데이트
        updateSkinFiles(updatedSkin)
        
        return SkinDto.SkinResponse(
            id = updatedSkin.id,
            name = updatedSkin.name,
            description = updatedSkin.description,
            type = updatedSkin.type.name,
            adminId = updatedSkin.adminId,
            isShared = updatedSkin.isShared,
            isOwner = updatedSkin.adminId == adminId,
            theme = parseTheme(updatedSkin.theme),
            styles = updatedSkin.styles ?: "",
            createdAt = updatedSkin.createdAt.toString(),
            updatedAt = updatedSkin.updatedAt.toString()
        )
    }
    
    /**
     * 스킨 삭제
     */
    fun deleteSkin(token: String, skinId: Long) {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("관리자를 찾을 수 없습니다.") }
        
        val skin = skinRepository.findById(skinId)
            .orElseThrow { IllegalArgumentException("스킨을 찾을 수 없습니다.") }
        
        // 권한 체크
        if (admin.role != AdminRole.SUPER_ADMIN && skin.adminId != adminId) {
            throw IllegalAccessException("스킨을 삭제할 권한이 없습니다.")
        }
        
        // 시스템 스킨은 삭제 불가
        if (skin.type == SkinType.BUILT_IN) {
            throw IllegalArgumentException("시스템 스킨은 삭제할 수 없습니다.")
        }
        
        // 파일 시스템에서 삭제
        deleteSkinFiles(skin)
        
        // DB에서 삭제
        skinRepository.delete(skin)
    }
    
    /**
     * 스킨 공유 토글
     */
    fun toggleSkinSharing(token: String, skinId: Long): SkinDto.SkinResponse {
        val adminId = extractAdminIdFromToken(token)
        
        val skin = skinRepository.findById(skinId)
            .orElseThrow { IllegalArgumentException("스킨을 찾을 수 없습니다.") }
        
        // 권한 체크: 본인이 만든 스킨만 공유 설정 가능
        if (skin.adminId != adminId) {
            throw IllegalAccessException("스킨 공유 설정을 변경할 권한이 없습니다.")
        }
        
        // 시스템 스킨은 공유 설정 변경 불가
        if (skin.type == SkinType.BUILT_IN) {
            throw IllegalArgumentException("시스템 스킨의 공유 설정은 변경할 수 없습니다.")
        }
        
        skin.isShared = !skin.isShared
        skin.type = if (skin.isShared) SkinType.SHARED else SkinType.PRIVATE
        skin.updatedAt = LocalDateTime.now()
        
        val updatedSkin = skinRepository.save(skin)
        
        return SkinDto.SkinResponse(
            id = updatedSkin.id,
            name = updatedSkin.name,
            description = updatedSkin.description,
            type = updatedSkin.type.name,
            adminId = updatedSkin.adminId,
            isShared = updatedSkin.isShared,
            isOwner = true,
            theme = parseTheme(updatedSkin.theme),
            styles = updatedSkin.styles ?: "",
            createdAt = updatedSkin.createdAt.toString(),
            updatedAt = updatedSkin.updatedAt.toString()
        )
    }
    
    /**
     * 파일 시스템에서 스킨 동기화
     */
    private fun syncSkinsFromFileSystem() {
        val skinsDir = File(skinsDirectory)
        if (!skinsDir.exists() || !skinsDir.isDirectory) {
            return
        }
        
        skinsDir.listFiles { file -> file.isDirectory }?.forEach { skinDir ->
            val themeFile = File(skinDir, "theme.js")
            val stylesFile = File(skinDir, "styles.js")
            
            if (themeFile.exists()) {
                val skinName = skinDir.name.split("-").joinToString(" ") { it.capitalize() }
                
                // 이미 존재하는 스킨인지 확인
                if (!skinRepository.existsByName(skinName)) {
                    val skin = Skin(
                        name = skinName,
                        description = getDefaultDescription(skinDir.name),
                        type = SkinType.BUILT_IN,
                        adminId = null,
                        isShared = true,
                        theme = "{}",  // 실제 테마는 프론트엔드에서 로드
                        styles = if (stylesFile.exists()) stylesFile.readText() else null,
                        folderName = skinDir.name
                    )
                    skinRepository.save(skin)
                }
            }
        }
    }
    
    private fun getDefaultDescription(skinName: String): String {
        return when (skinName) {
            "minimal" -> "깔끔하고 심플한 기본 스킨"
            "dark" -> "다크 모드를 위한 어두운 테마"
            "colorful" -> "생동감 있는 컬러풀 테마"
            "modern" -> "모던하고 세련된 그라데이션 테마"
            "compact" -> "간결하고 효율적인 레이아웃"
            else -> "$skinName 스킨"
        }
    }
    
    private fun generateSkinFileName(name: String): String {
        return name.lowercase().replace(" ", "-")
    }
    
    private fun parseTheme(themeJson: String?): SkinDto.SkinTheme {
        return try {
            objectMapper.readValue(themeJson ?: "{}")
        } catch (e: Exception) {
            // 기본 테마 반환
            SkinDto.SkinTheme(
                colors = SkinDto.SkinColors(
                    primary = "#2563eb",
                    primaryHover = "#1d4ed8",
                    secondary = "#64748b",
                    background = "#ffffff",
                    surface = "#f8fafc",
                    textPrimary = "#1e293b",
                    textSecondary = "#475569",
                    border = "#e2e8f0"
                ),
                fonts = SkinDto.SkinFonts(
                    family = "-apple-system, BlinkMacSystemFont, sans-serif",
                    sizeBase = "14px",
                    sizeLg = "16px"
                ),
                spacing = SkinDto.SkinSpacing(
                    sm = "8px",
                    md = "16px",
                    lg = "24px"
                ),
                borderRadius = SkinDto.SkinBorderRadius(
                    sm = "6px",
                    md = "8px"
                )
            )
        }
    }
    
    private fun createSkinFiles(skin: Skin) {
        val skinDir = File(skinsDirectory, skin.folderName)
        skinDir.mkdirs()
        
        // theme.js 파일 생성
        val themeFile = File(skinDir, "theme.js")
        val theme = parseTheme(skin.theme)
        themeFile.writeText(generateThemeJs(theme))
        
        // styles.js 파일 생성
        val stylesFile = File(skinDir, "styles.js")
        stylesFile.writeText(skin.styles ?: generateDefaultStyles())
    }
    
    private fun updateSkinFiles(skin: Skin) {
        // 기존 파일 업데이트
        createSkinFiles(skin)
    }
    
    private fun deleteSkinFiles(skin: Skin) {
        val skinDir = File(skinsDirectory, skin.folderName)
        if (skinDir.exists()) {
            skinDir.deleteRecursively()
        }
    }
    
    private fun generateThemeJs(theme: SkinDto.SkinTheme): String {
        return """
window.CommentWidgetThemes = window.CommentWidgetThemes || {};
window.CommentWidgetThemes['${theme.colors.primary}'] = {
    colors: {
        primary: '${theme.colors.primary}',
        primaryHover: '${theme.colors.primaryHover}',
        secondary: '${theme.colors.secondary}',
        background: '${theme.colors.background}',
        surface: '${theme.colors.surface}',
        textPrimary: '${theme.colors.textPrimary}',
        textSecondary: '${theme.colors.textSecondary}',
        border: '${theme.colors.border}'
    },
    fonts: {
        family: '${theme.fonts.family}',
        sizeBase: '${theme.fonts.sizeBase}',
        sizeLg: '${theme.fonts.sizeLg}'
    },
    spacing: {
        sm: '${theme.spacing.sm}',
        md: '${theme.spacing.md}',
        lg: '${theme.spacing.lg}'
    },
    borderRadius: {
        sm: '${theme.borderRadius.sm}',
        md: '${theme.borderRadius.md}'
    }
};
        """.trimIndent()
    }
    
    private fun generateDefaultStyles(): String {
        return """
/* Custom styles for this skin */
        """.trimIndent()
    }
    
    /**
     * 사이트별 스킨 적용 상황 조회
     */
    fun getSkinApplicationsBySite(token: String, siteId: String): List<SkinDto.SkinApplyResponse> {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("관리자를 찾을 수 없습니다.") }
        
        // 권한 체크: 슈퍼 관리자이거나 해당 사이트의 관리자인 경우만 조회 가능
        if (admin.role != AdminRole.SUPER_ADMIN) {
            // TODO: 실제 운영 시에는 사이트별 관리자 권한 체크 로직 추가
        }
        
        val applications = skinApplicationRepository.findBySiteId(siteId)
        return applications.map { application ->
            SkinDto.SkinApplyResponse(
                id = application.id,
                siteKey = application.siteId,
                pageId = application.pageId,
                skinName = application.skinName,
                scope = application.scope,
                createdAt = application.createdAt,
                updatedAt = application.updatedAt
            )
        }
    }
    
    /**
     * 게시판별 스킨 적용
     */
    fun applySkinToPage(token: String, request: SkinDto.SkinApplyRequest): SkinDto.SkinApplyResponse {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("관리자를 찾을 수 없습니다.") }
        
        // 스킨 존재 확인
        if (!skinRepository.existsByName(request.skinName)) {
            throw IllegalArgumentException("존재하지 않는 스킨입니다: ${request.skinName}")
        }
        
        // 기존 적용 설정 확인
        val existingApplication = skinApplicationRepository
            .findBySiteIdAndPageId(request.siteKey, request.pageId)
        
        val application = if (existingApplication.isPresent) {
            // 기존 설정 업데이트
            val existing = existingApplication.get()
            existing.skinName = request.skinName
            existing.scope = request.scope
            existing.updatedAt = LocalDateTime.now()
            skinApplicationRepository.save(existing)
        } else {
            // 새 설정 생성
            val newApplication = SkinApplication(
                siteId = request.siteKey,
                pageId = request.pageId,
                skinName = request.skinName,
                scope = request.scope
            )
            skinApplicationRepository.save(newApplication)
        }
        
        return SkinDto.SkinApplyResponse(
            id = application.id,
            siteKey = application.siteId,
            pageId = application.pageId,
            skinName = application.skinName,
            scope = application.scope,
            createdAt = application.createdAt,
            updatedAt = application.updatedAt
        )
    }
    
    /**
     * 스킨 적용 해제
     */
    fun removeSkinApplication(token: String, applicationId: Long) {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("관리자를 찾을 수 없습니다.") }
        
        val application = skinApplicationRepository.findById(applicationId)
            .orElseThrow { IllegalArgumentException("스킨 적용 설정을 찾을 수 없습니다.") }
        
        // 권한 체크: 슈퍼 관리자이거나 해당 사이트의 관리자인 경우만 삭제 가능
        if (admin.role != AdminRole.SUPER_ADMIN) {
            // TODO: 실제 운영 시에는 사이트별 관리자 권한 체크 로직 추가
        }
        
        skinApplicationRepository.delete(application)
    }
    
    /**
     * 사이트의 모든 게시판에 스킨 일괄 적용
     */
    fun applySkinToAllPages(token: String, request: SkinDto.SkinBulkApplyRequest): List<SkinDto.SkinApplyResponse> {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("관리자를 찾을 수 없습니다.") }
        
        // 스킨 존재 확인
        if (!skinRepository.existsByName(request.skinName)) {
            throw IllegalArgumentException("존재하지 않는 스킨입니다: ${request.skinName}")
        }
        
        val results = mutableListOf<SkinDto.SkinApplyResponse>()
        
        if (request.pageIds.isNullOrEmpty()) {
            // 전체 사이트에 적용
            val applyRequest = SkinDto.SkinApplyRequest(
                siteKey = request.siteKey,
                pageId = null,
                skinName = request.skinName,
                scope = ApplicationScope.ALL
            )
            results.add(applySkinToPage(token, applyRequest))
        } else {
            // 지정된 게시판들에 적용
            for (pageId in request.pageIds) {
                try {
                    val existingApplication = skinApplicationRepository
                        .findBySiteIdAndPageId(request.siteKey, pageId)
                    
                    // 기존 설정이 있고 덮어쓰기를 허용하지 않는 경우 스킵
                    if (existingApplication.isPresent && !request.overwriteExisting) {
                        continue
                    }
                    
                    val applyRequest = SkinDto.SkinApplyRequest(
                        siteKey = request.siteKey,
                        pageId = pageId,
                        skinName = request.skinName,
                        scope = ApplicationScope.SPECIFIC
                    )
                    results.add(applySkinToPage(token, applyRequest))
                } catch (e: Exception) {
                    // 개별 페이지 적용 실패 시 로그만 남기고 계속 진행
                    println("Failed to apply skin to page $pageId: ${e.message}")
                }
            }
        }
        
        return results
    }
    
    /**
     * 관리자의 사이트 관리 현황 조회 (사이트, 페이지, 스킨 적용 상태 모두 포함)
     */
    fun getSiteManagementOverview(token: String): SkinDto.SiteManagementResponse {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("관리자를 찾을 수 없습니다.") }
        
        val managedSites = if (admin.role == AdminRole.SUPER_ADMIN) {
            // 슈퍼 관리자: 모든 사이트
            getAllSitesWithDetails()
        } else {
            // 일반 관리자: 권한이 있는 사이트만
            getAuthorizedSitesWithDetails(adminId)
        }
        
        return SkinDto.SiteManagementResponse(
            sites = managedSites,
            totalSites = siteRepository.findByIsActiveTrue().size,
            managedSites = managedSites.size
        )
    }
    
    /**
     * 특정 사이트의 상세 정보 및 페이지별 스킨 적용 상태 조회
     */
    fun getSiteDetailWithSkinStatus(token: String, siteId: String): SkinDto.AdminSiteInfo {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("관리자를 찾을 수 없습니다.") }
        
        // 권한 체크
        if (admin.role != AdminRole.SUPER_ADMIN) {
            val hasPermission = adminSitePermissionRepository
                .existsByAdminIdAndSiteIdAndIsActiveTrue(adminId, siteId)
            if (!hasPermission) {
                throw IllegalAccessException("해당 사이트에 대한 관리 권한이 없습니다.")
            }
        }
        
        // 실제 사이트 정보 조회
        val site = siteRepository.findBySiteKey(siteId)
        return getSiteInfoWithPages(siteId, adminId, null, site)
    }
    
    /**
     * 페이지 정보 등록/업데이트 (자동 발견된 페이지에 대한 메타데이터 추가)
     */
    fun registerOrUpdatePage(token: String, siteId: String, pageId: String, pageName: String?, pageDescription: String?, pageType: PageType = PageType.BOARD): SkinDto.SitePageInfo {
        val adminId = extractAdminIdFromToken(token)
        
        // 권한 체크
        checkSitePermission(adminId, siteId)
        
        val existingPage = sitePageRepository.findBySiteIdAndPageIdAndIsActiveTrue(siteId, pageId)
        
        val page = if (existingPage != null) {
            // 기존 페이지 업데이트
            existingPage.pageName = pageName
            existingPage.pageDescription = pageDescription
            existingPage.pageType = pageType
            existingPage.updatedAt = LocalDateTime.now()
            sitePageRepository.save(existingPage)
        } else {
            // 새 페이지 등록
            val newPage = SitePage(
                siteId = siteId,
                pageId = pageId,
                pageName = pageName,
                pageDescription = pageDescription,
                pageType = pageType
            )
            sitePageRepository.save(newPage)
        }
        
        // 현재 적용된 스킨 정보 조회
        val skinApplication = skinApplicationRepository.findBySiteIdAndPageId(siteId, pageId)
        
        return SkinDto.SitePageInfo(
            pageId = page.pageId,
            pageName = page.pageName,
            pageDescription = page.pageDescription,
            pageType = page.pageType.name,
            commentCount = page.commentCount,
            appliedSkin = skinApplication.orElse(null)?.skinName,
            skinScope = skinApplication.orElse(null)?.scope?.name,
            lastActivityAt = page.lastActivityAt,
            isActive = page.isActive
        )
    }
    
    /**
     * 관리 가능한 사이트 목록 조회 (기존 메서드 호환성 유지)
     */
    fun getManageableSites(token: String): List<String> {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("관리자를 찾을 수 없습니다.") }
        
        return if (admin.role == AdminRole.SUPER_ADMIN) {
            // 슈퍼 관리자는 모든 사이트 조회
            siteRepository.findByIsActiveTrue().map { it.siteKey }
        } else {
            // 일반 관리자는 자신이 소유한 사이트만 조회
            siteRepository.findByOwnerId(adminId).filter { it.isActive }.map { it.siteKey }
        }
    }
    
    // === 헬퍼 메서드들 ===
    
    private fun getAllSitesWithDetails(): List<SkinDto.AdminSiteInfo> {
        val allSites = siteRepository.findByIsActiveTrue()
        return allSites.map { site ->
            getSiteInfoWithPages(site.siteKey, null, null, site)
        }
    }
    
    private fun getAuthorizedSitesWithDetails(adminId: Long): List<SkinDto.AdminSiteInfo> {
        // 현재 관리자가 소유한 사이트들 조회
        val ownedSites = siteRepository.findByOwnerId(adminId)
        return ownedSites.filter { it.isActive }.map { site ->
            getSiteInfoWithPages(site.siteKey, adminId, null, site)
        }
    }
    
    private fun getSiteInfoWithPages(siteId: String, adminId: Long?, permission: AdminSitePermission? = null, site: Site? = null): SkinDto.AdminSiteInfo {
        // 사이트의 모든 페이지 조회
        val pages = sitePageRepository.findBySiteIdAndIsActiveTrue(siteId)
        
        // 각 페이지별 스킨 적용 상태 조회
        val skinApplications = skinApplicationRepository.findBySiteId(siteId)
            .associateBy { "${it.siteId}:${it.pageId}" }
        
        // 전체 사이트 스킨 적용 조회
        val siteWideSkin = skinApplications["${siteId}:null"]
        
        val pageInfos = pages.map { page ->
            val applicationKey = "${page.siteId}:${page.pageId}"
            val pageApplication = skinApplications[applicationKey]
            
            SkinDto.SitePageInfo(
                pageId = page.pageId,
                pageName = page.pageName,
                pageDescription = page.pageDescription,
                pageType = page.pageType.name,
                commentCount = page.commentCount,
                appliedSkin = pageApplication?.skinName ?: siteWideSkin?.skinName,
                skinScope = pageApplication?.scope?.name ?: siteWideSkin?.scope?.name,
                lastActivityAt = page.lastActivityAt,
                isActive = page.isActive
            )
        }
        
        val lastActivity = pages.mapNotNull { it.lastActivityAt }.maxOrNull()
        
        return SkinDto.AdminSiteInfo(
            siteId = siteId,
            siteName = site?.siteName ?: permission?.siteName,
            siteDescription = null, // Site entity doesn't have description field
            permission = permission?.permission?.name ?: (if (adminId != null && site?.ownerId == adminId) "OWNER" else "SUPER_ADMIN"),
            pageCount = pages.size.toLong(),
            lastActivityAt = lastActivity,
            pages = pageInfos
        )
    }
    
    private fun checkSitePermission(adminId: Long, siteId: String) {
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("관리자를 찾을 수 없습니다.") }
        
        if (admin.role != AdminRole.SUPER_ADMIN) {
            val hasPermission = adminSitePermissionRepository
                .existsByAdminIdAndSiteIdAndIsActiveTrue(adminId, siteId)
            if (!hasPermission) {
                throw IllegalAccessException("해당 사이트에 대한 관리 권한이 없습니다.")
            }
        }
    }
}