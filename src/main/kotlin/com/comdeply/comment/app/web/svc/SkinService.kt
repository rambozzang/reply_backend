package com.comdeply.comment.app.web.svc

import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.Skin
import com.comdeply.comment.entity.SkinApplication
import com.comdeply.comment.entity.SkinType
import com.comdeply.comment.repository.SkinApplicationRepository
import com.comdeply.comment.repository.SkinRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@Service
@Transactional
class SkinService(
    private val skinRepository: SkinRepository,
    private val skinApplicationRepository: SkinApplicationRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${app.skin.directory:/frontend/public/comment-widget-skins}")
    private val skinDirectory: String
) {
    private val logger = LoggerFactory.getLogger(SkinService::class.java)

    fun getAllSkins(): List<SkinDto.SkinResponse> {
        return skinRepository.findAll().map { it.toResponseDto() }
    }

    /**
     * 관리자가 접근 가능한 스킨 목록 조회 (공통 스킨 + 자신의 개인 스킨)
     */
    fun getSkinsForAdmin(adminId: Long): List<SkinDto.SkinResponse> {
        // BUILT_IN, SHARED 스킨과 해당 관리자의 PRIVATE 스킨을 조회
        val publicSkins = skinRepository.findByTypeIn(listOf(SkinType.BUILT_IN, SkinType.SHARED))
        val privateSkins = skinRepository.findByAdminId(adminId)

        return (publicSkins + privateSkins)
            .distinctBy { it.id }
            .map { it.toResponseDto(adminId) }
    }

    /**
     * 관리자의 개인 스킨만 조회
     */
    fun getPrivateSkinsForAdmin(adminId: Long): List<SkinDto.SkinResponse> {
        return skinRepository.findByAdminId(adminId)
            .map { it.toResponseDto(adminId) }
    }

    fun getSkinById(id: Long): SkinDto.SkinResponse {
        val skin = skinRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Skin not found with id: $id") }
        return skin.toResponseDto()
    }

    fun getSkinByName(name: String): SkinDto.SkinResponse {
        val skin = skinRepository.findByName(name)
            .orElseThrow { IllegalArgumentException("Skin not found with name: $name") }
        return skin.toResponseDto()
    }

    fun createSkin(request: SkinDto.CreateSkinRequest, adminId: Long): SkinDto.SkinResponse {
        // 이름 중복 체크
        if (skinRepository.existsByName(request.name)) {
            throw IllegalArgumentException("Skin with name '${request.name}' already exists")
        }

        // 폴더명 생성 (소문자, 공백을 하이픈으로)
        val folderName = request.name.lowercase().replace("\\s+".toRegex(), "-")

        if (skinRepository.existsByFolderName(folderName)) {
            throw IllegalArgumentException("Skin with folder name '$folderName' already exists")
        }

        val themeJson = objectMapper.writeValueAsString(request.theme)

        val skin = Skin(
            name = request.name,
            description = request.description,
            type = SkinType.PRIVATE,
            adminId = adminId,
            isShared = false,
            theme = themeJson,
            styles = request.styles,
            folderName = folderName
        )

        val savedSkin = skinRepository.save(skin)

        // 파일 시스템에 스킨 파일 생성
        createSkinFiles(savedSkin)

        return savedSkin.toResponseDto()
    }

    fun updateSkin(id: Long, request: SkinDto.UpdateSkinRequest, adminId: Long): SkinDto.SkinResponse {
        val skin = skinRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Skin not found with id: $id") }

        // BUILT_IN 스킨은 수정 불가
        if (skin.type == SkinType.BUILT_IN) {
            throw IllegalArgumentException("Built-in skins cannot be modified")
        }

        // 자신의 스킨만 수정 가능 (SHARED 스킨도 원래 소유자만 수정 가능)
        if (skin.adminId != adminId) {
            throw IllegalArgumentException("You can only modify your own skins")
        }

        val updatedSkin = skin.copy(
            name = request.name ?: skin.name,
            description = request.description ?: skin.description,
            theme = request.theme?.let { objectMapper.writeValueAsString(it) } ?: skin.theme,
            styles = request.styles ?: skin.styles
        )

        val savedSkin = skinRepository.save(updatedSkin)

        // 파일 시스템 업데이트
        createSkinFiles(savedSkin)

        return savedSkin.toResponseDto()
    }

    /**
     * 스킨 공유 상태 변경
     */
    fun toggleSkinSharing(id: Long, adminId: Long): SkinDto.SkinResponse {
        val skin = skinRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Skin not found with id: $id") }

        // BUILT_IN 스킨은 공유 상태 변경 불가
        if (skin.type == SkinType.BUILT_IN) {
            throw IllegalArgumentException("Built-in skins cannot be shared/unshared")
        }

        // 자신의 스킨만 공유 상태 변경 가능
        if (skin.adminId != adminId) {
            throw IllegalArgumentException("You can only share/unshare your own skins")
        }

        val updatedSkin = if (skin.isShared) {
            // 공유 해제: SHARED -> PRIVATE
            skin.copy(
                type = SkinType.PRIVATE,
                isShared = false
            )
        } else {
            // 공유 활성화: PRIVATE -> SHARED
            skin.copy(
                type = SkinType.SHARED,
                isShared = true
            )
        }

        val savedSkin = skinRepository.save(updatedSkin)
        return savedSkin.toResponseDto()
    }

    fun deleteSkin(id: Long, adminId: Long) {
        val skin = skinRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Skin not found with id: $id") }

        // BUILT_IN 스킨은 삭제 불가
        if (skin.type == SkinType.BUILT_IN) {
            throw IllegalArgumentException("Built-in skins cannot be deleted")
        }

        // 자신의 스킨만 삭제 가능
        if (skin.adminId != adminId) {
            throw IllegalArgumentException("You can only delete your own skins")
        }

        // 사용중인 스킨인지 확인
        val applications = skinApplicationRepository.findBySkinName(skin.name)
        if (applications.isNotEmpty()) {
            throw IllegalArgumentException("Cannot delete skin '${skin.name}' as it is currently in use")
        }

        // 파일 시스템에서 폴더 삭제
        deleteSkinFiles(skin.folderName)

        skinRepository.delete(skin)
    }

    fun applySkin(request: SkinDto.SkinApplyRequest): SkinDto.SkinApplyResponse {
        // 스킨 존재 확인
        skinRepository.findByName(request.skinName)
            ?: throw IllegalArgumentException("Skin not found with name: ${request.skinName}")

        // 기존 적용 설정 확인
        val existingApplication = skinApplicationRepository
            .findBySiteIdAndPageId(request.siteKey, request.pageId)

        val application = if (existingApplication.isPresent) {
            // 기존 설정 업데이트
            val existing = existingApplication.get()
            existing.copy(
                skinName = request.skinName,
                scope = request.scope
            )
        } else {
            // 새 설정 생성
            SkinApplication(
                siteId = request.siteKey,
                pageId = request.pageId,
                skinName = request.skinName,
                scope = request.scope
            )
        }

        val savedApplication = skinApplicationRepository.save(application)
        return savedApplication.toResponseDto()
    }

    fun getSkinApplications(siteId: String): List<SkinDto.SkinApplyResponse> {
        return skinApplicationRepository.findBySiteId(siteId)
            .map { it.toResponseDto() }
    }

    fun scanAndSyncExistingSkinsFromFileSystem(): Map<String, Any> {
        logger.info("Scanning existing skins from file system...")

        val skinDir = File(skinDirectory)
        if (!skinDir.exists() || !skinDir.isDirectory) {
            logger.warn("Skin directory does not exist: $skinDirectory")
            return mapOf(
                "error" to "Skin directory does not exist: $skinDirectory",
                "exists" to false,
                "isDirectory" to false
            )
        }

        val builtInSkins = mapOf(
            "minimal" to "깔끔하고 심플한 기본 스킨",
            "dark" to "다크 모드를 위한 어두운 테마",
            "colorful" to "생동감 있는 컬러풀 테마",
            "modern" to "모던하고 세련된 그라데이션 테마",
            "compact" to "간결하고 효율적인 레이아웃"
        )

        val result = mutableMapOf<String, Any>()
        result["skinDirectory"] = skinDirectory
        result["directoryExists"] = skinDir.exists()
        result["isDirectory"] = skinDir.isDirectory

        val files = skinDir.listFiles()
        result["filesFound"] = files?.map { it.name } ?: emptyList<String>()

        val processedSkins = mutableListOf<String>()
        val skippedSkins = mutableListOf<String>()
        val errorSkins = mutableListOf<String>()

        files?.forEach { folder ->
            if (folder.isDirectory && folder.name !in listOf(".", "..")) {
                val folderName = folder.name
                val themeFile = File(folder, "theme.js")
                val stylesFile = File(folder, "styles.js")

                if (themeFile.exists() && stylesFile.exists()) {
                    try {
                        val theme = extractThemeFromFile(themeFile)
                        val styles = extractStylesFromFile(stylesFile)

                        // DB에 이미 존재하는지 확인
                        val existingSkin = skinRepository.findByFolderName(folderName)
                        if (existingSkin.isPresent) {
                            // 기존 스킨 업데이트 (theme과 styles만)
                            val updatedSkin = existingSkin.get().copy(
                                theme = objectMapper.writeValueAsString(theme),
                                styles = styles
                            )
                            skinRepository.save(updatedSkin)
                            logger.info("Updated skin from file system: $folderName")
                            processedSkins.add("$folderName (updated)")
                        } else {
                            // 새 스킨 생성
                            val skin = Skin(
                                name = folderName.replaceFirstChar { it.uppercase() },
                                description = builtInSkins[folderName] ?: "$folderName 스킨",
                                type = if (folderName in builtInSkins.keys) SkinType.BUILT_IN else SkinType.PRIVATE,
                                adminId = if (folderName in builtInSkins.keys) null else 1L, // 임시로 admin ID 1 사용
                                isShared = folderName in builtInSkins.keys,
                                theme = objectMapper.writeValueAsString(theme),
                                styles = styles,
                                folderName = folderName
                            )

                            skinRepository.save(skin)
                            logger.info("Added skin from file system: $folderName")
                            processedSkins.add("$folderName (new)")
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to process skin folder: $folderName", e)
                        errorSkins.add("$folderName: ${e.message}")
                    }
                } else {
                    skippedSkins.add("$folderName (missing theme.js or styles.js)")
                }
            }
        }

        result["processedSkins"] = processedSkins
        result["skippedSkins"] = skippedSkins
        result["errorSkins"] = errorSkins

        return result
    }

    private fun createSkinFiles(skin: Skin) {
        try {
            val skinPath = Paths.get(skinDirectory, skin.folderName)
            Files.createDirectories(skinPath)

            // theme.js 파일 생성
            val themeFileName = "${skin.folderName}Theme"
            val themeContent = """
                export const $themeFileName = ${skin.theme};

                export function generateThemeCss(theme) {
                  let css = ':root {\n';
                  for (const category in theme) {
                    for (const key in theme[category]) {
                      const varName = `--comdeply-${'$'}{category}-${'$'}{key}`;
                      css += `  ${'$'}{varName}: ${'$'}{theme[category][key]};\n`;
                    }
                  }
                  css += '}';
                  return css;
                }
            """.trimIndent()

            Files.write(
                skinPath.resolve("theme.js"),
                themeContent.toByteArray(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )

            // styles.js 파일 생성
            val stylesFileName = "${skin.folderName}Styles"
            val stylesContent = "export const $stylesFileName = `${skin.styles}`;"

            Files.write(
                skinPath.resolve("styles.js"),
                stylesContent.toByteArray(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )

            logger.info("Created skin files for: ${skin.folderName}")
        } catch (e: Exception) {
            logger.error("Failed to create skin files for: ${skin.folderName}", e)
            throw RuntimeException("Failed to create skin files", e)
        }
    }

    private fun deleteSkinFiles(folderName: String) {
        try {
            val skinPath = Paths.get(skinDirectory, folderName)
            if (Files.exists(skinPath)) {
                Files.walk(skinPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.delete(it) }
                logger.info("Deleted skin files for: $folderName")
            }
        } catch (e: Exception) {
            logger.error("Failed to delete skin files for: $folderName", e)
        }
    }

    private fun extractThemeFromFile(themeFile: File): Map<String, Any> {
        // 간단한 정규식 대신 폴더 이름으로 테마 매핑
        val folderName = themeFile.parentFile.name
        return getPredefinedTheme(folderName)
    }

    private fun getPredefinedTheme(folderName: String): Map<String, Any> {
        return when (folderName) {
            "colorful" -> mapOf(
                "colors" to mapOf(
                    "primary" to "#f59e0b",
                    "primaryHover" to "#d97706",
                    "secondary" to "#ef4444",
                    "secondaryHover" to "#dc2626",
                    "background" to "#fef3c7",
                    "surface" to "#fef7ed",
                    "textPrimary" to "#92400e",
                    "textSecondary" to "#b45309",
                    "textMuted" to "#d97706",
                    "border" to "#fed7aa",
                    "error" to "#dc2626",
                    "success" to "#059669",
                    "voteButton" to "#fef3c7",
                    "voteButtonHover" to "#fed7aa",
                    "voteButtonActive" to "#f59e0b",
                    "voteButtonColor" to "#92400e",
                    "voteButtonActiveColor" to "#ffffff",
                    "voteButtonBorder" to "#fed7aa",
                    "voteButtonActiveBorder" to "#f59e0b"
                ),
                "fonts" to mapOf(
                    "family" to "'Comic Sans MS', cursive, -apple-system, BlinkMacSystemFont, sans-serif",
                    "sizeBase" to "15px",
                    "sizeSm" to "13px",
                    "sizeLg" to "20px",
                    "weightLight" to "300",
                    "weightNormal" to "400",
                    "weightMedium" to "600",
                    "weightBold" to "700"
                ),
                "spacing" to mapOf(
                    "xs" to "6px",
                    "sm" to "10px",
                    "md" to "18px",
                    "lg" to "24px",
                    "xl" to "30px"
                ),
                "borderRadius" to mapOf(
                    "sm" to "8px",
                    "md" to "12px",
                    "lg" to "16px"
                )
            )
            "dark" -> mapOf(
                "colors" to mapOf(
                    "primary" to "#3b82f6",
                    "primaryHover" to "#2563eb",
                    "secondary" to "#6b7280",
                    "secondaryHover" to "#4b5563",
                    "background" to "#1f2937",
                    "surface" to "#374151",
                    "textPrimary" to "#f9fafb",
                    "textSecondary" to "#d1d5db",
                    "textMuted" to "#9ca3af",
                    "border" to "#4b5563",
                    "error" to "#ef4444",
                    "success" to "#10b981",
                    "voteButton" to "transparent",
                    "voteButtonHover" to "#4b5563",
                    "voteButtonActive" to "#3b82f6",
                    "voteButtonColor" to "#9ca3af",
                    "voteButtonActiveColor" to "#ffffff",
                    "voteButtonBorder" to "#6b7280",
                    "voteButtonActiveBorder" to "#3b82f6"
                ),
                "fonts" to mapOf(
                    "family" to "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif",
                    "sizeBase" to "14px",
                    "sizeSm" to "12px",
                    "sizeLg" to "18px",
                    "weightLight" to "300",
                    "weightNormal" to "400",
                    "weightMedium" to "500",
                    "weightBold" to "700"
                ),
                "spacing" to mapOf(
                    "xs" to "4px",
                    "sm" to "8px",
                    "md" to "16px",
                    "lg" to "20px",
                    "xl" to "24px"
                ),
                "borderRadius" to mapOf(
                    "sm" to "4px",
                    "md" to "8px",
                    "lg" to "12px"
                )
            )
            "modern" -> mapOf(
                "colors" to mapOf(
                    "primary" to "#8b5cf6",
                    "primaryHover" to "#7c3aed",
                    "secondary" to "#10b981",
                    "background" to "#fefefe",
                    "surface" to "#f3f4f6",
                    "textPrimary" to "#111827",
                    "textSecondary" to "#6b7280",
                    "border" to "#d1d5db"
                ),
                "fonts" to mapOf(
                    "family" to "'Inter', -apple-system, BlinkMacSystemFont, sans-serif",
                    "sizeBase" to "14px",
                    "sizeLg" to "18px"
                ),
                "spacing" to mapOf(
                    "sm" to "8px",
                    "md" to "16px",
                    "lg" to "24px"
                ),
                "borderRadius" to mapOf(
                    "sm" to "8px",
                    "md" to "12px"
                )
            )
            "compact" -> mapOf(
                "colors" to mapOf(
                    "primary" to "#059669",
                    "primaryHover" to "#047857",
                    "secondary" to "#6b7280",
                    "background" to "#f0fdf4",
                    "surface" to "#dcfce7",
                    "textPrimary" to "#14532d",
                    "textSecondary" to "#16a34a",
                    "border" to "#bbf7d0"
                ),
                "fonts" to mapOf(
                    "family" to "-apple-system, BlinkMacSystemFont, sans-serif",
                    "sizeBase" to "13px",
                    "sizeLg" to "15px"
                ),
                "spacing" to mapOf(
                    "sm" to "6px",
                    "md" to "12px",
                    "lg" to "18px"
                ),
                "borderRadius" to mapOf(
                    "sm" to "4px",
                    "md" to "6px"
                )
            )
            else -> getDefaultTheme()
        }
    }

    private fun extractStylesFromFile(stylesFile: File): String {
        val content = stylesFile.readText()
        val stylesMatch = Regex("export const \\w+Styles = `([\\s\\S]*?)`;").find(content)
        return stylesMatch?.groupValues?.get(1) ?: ""
    }

    private fun getDefaultTheme(): Map<String, Any> {
        return mapOf(
            "colors" to mapOf(
                "primary" to "#2563eb",
                "primaryHover" to "#1d4ed8",
                "secondary" to "#64748b",
                "background" to "#ffffff",
                "surface" to "#f8fafc",
                "textPrimary" to "#1e293b",
                "textSecondary" to "#475569",
                "border" to "#e2e8f0"
            ),
            "fonts" to mapOf(
                "family" to "-apple-system, BlinkMacSystemFont, sans-serif",
                "sizeBase" to "14px",
                "sizeLg" to "16px"
            ),
            "spacing" to mapOf(
                "sm" to "8px",
                "md" to "16px",
                "lg" to "24px"
            ),
            "borderRadius" to mapOf(
                "sm" to "6px",
                "md" to "8px"
            )
        )
    }

    private fun getDefaultSkinTheme(): SkinDto.SkinTheme {
        return SkinDto.SkinTheme(
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

    private fun Skin.toResponseDto(currentAdminId: Long? = null): SkinDto.SkinResponse {
        val themeObject = try {
            objectMapper.readValue(this.theme, SkinDto.SkinTheme::class.java)
        } catch (e: Exception) {
            getDefaultSkinTheme()
        }
        return SkinDto.SkinResponse(
            id = this.id,
            name = this.name,
            description = this.description,
            type = this.type.name,
            adminId = this.adminId,
            isShared = this.isShared,
            isOwner = currentAdminId != null && this.adminId == currentAdminId,
            theme = themeObject,
            styles = this.styles?: "",
            createdAt = this.createdAt.toString(),
            updatedAt = this.updatedAt.toString()
        )
    }

    private fun SkinApplication.toResponseDto(): SkinDto.SkinApplyResponse {
        return SkinDto.SkinApplyResponse(
            id = this.id,
            siteKey = this.siteId,
            pageId = this.pageId,
            skinName = this.skinName,
            scope = this.scope,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}
