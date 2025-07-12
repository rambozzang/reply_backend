package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.ThemeService
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "테마", description = "테마 관리 API")
@RestController
@RequestMapping("/themes")
@CrossOrigin(originPatterns = ["*"])
class ThemeController(
    private val themeService: ThemeService
) {

    @Operation(
        summary = "모든 활성 테마 조회",
        description = "활성화된 모든 테마를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "테마 목록 조회 성공")
        ]
    )
    @GetMapping
    fun getAllActiveThemes(): ResponseEntity<List<ThemeResponse>> {
        val themes = themeService.getAllActiveThemes()
        return ResponseEntity.ok(themes)
    }

    @Operation(
        summary = "테마 페이징 조회",
        description = "페이징을 적용하여 테마를 조회합니다."
    )
    @GetMapping("/paged")
    fun getThemesPaged(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,

        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int,

        @Parameter(description = "정렬 기준", example = "usageCount")
        @RequestParam(defaultValue = "usageCount")
        sort: String,

        @Parameter(description = "정렬 방향", example = "DESC")
        @RequestParam(defaultValue = "DESC")
        direction: String
    ): ResponseEntity<Page<ThemeResponse>> {
        val sortDirection = Sort.Direction.fromString(direction)
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort))
        val themes = themeService.getThemes(pageable)
        return ResponseEntity.ok(themes)
    }

    @Operation(
        summary = "테마 상세 조회",
        description = "특정 테마의 상세 정보를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "테마 조회 성공"),
            ApiResponse(responseCode = "404", description = "테마를 찾을 수 없음")
        ]
    )
    @GetMapping("/{themeId}")
    fun getThemeById(
        @Parameter(description = "테마 ID", required = true)
        @PathVariable
        themeId: Long
    ): ResponseEntity<ThemeResponse> {
        return try {
            val theme = themeService.getThemeById(themeId)
            ResponseEntity.ok(theme)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "테마 이름으로 조회",
        description = "테마 이름으로 특정 테마를 조회합니다."
    )
    @GetMapping("/by-name/{name}")
    fun getThemeByName(
        @Parameter(description = "테마 이름", required = true)
        @PathVariable
        name: String
    ): ResponseEntity<ThemeResponse> {
        val theme = themeService.getThemeByName(name)
        return if (theme != null) {
            ResponseEntity.ok(theme)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "카테고리별 테마 조회",
        description = "특정 카테고리의 테마를 조회합니다."
    )
    @GetMapping("/category/{category}")
    fun getThemesByCategory(
        @Parameter(description = "카테고리 이름", required = true)
        @PathVariable
        category: String,

        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,

        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int
    ): ResponseEntity<Page<ThemeResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val themes = themeService.getThemesByCategory(category, pageable)
        return ResponseEntity.ok(themes)
    }

    @Operation(
        summary = "인기 테마 조회",
        description = "사용량 기준으로 인기 테마를 조회합니다."
    )
    @GetMapping("/popular")
    fun getPopularThemes(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,

        @Parameter(description = "페이지 크기", example = "10")
        @RequestParam(defaultValue = "10")
        size: Int
    ): ResponseEntity<Page<ThemeResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val themes = themeService.getPopularThemes(pageable)
        return ResponseEntity.ok(themes)
    }

    @Operation(
        summary = "최신 테마 조회",
        description = "최근에 생성된 테마를 조회합니다."
    )
    @GetMapping("/latest")
    fun getLatestThemes(
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,

        @Parameter(description = "페이지 크기", example = "10")
        @RequestParam(defaultValue = "10")
        size: Int
    ): ResponseEntity<Page<ThemeResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val themes = themeService.getLatestThemes(pageable)
        return ResponseEntity.ok(themes)
    }

    @Operation(
        summary = "기본 테마 조회",
        description = "시스템에서 제공하는 기본 테마를 조회합니다."
    )
    @GetMapping("/built-in")
    fun getBuiltInThemes(): ResponseEntity<List<ThemeResponse>> {
        val themes = themeService.getBuiltInThemes()
        return ResponseEntity.ok(themes)
    }

    @Operation(
        summary = "프리미엄 테마 조회",
        description = "프리미엄 테마를 조회합니다."
    )
    @GetMapping("/premium")
    fun getPremiumThemes(): ResponseEntity<List<ThemeResponse>> {
        val themes = themeService.getPremiumThemes()
        return ResponseEntity.ok(themes)
    }

    @Operation(
        summary = "사용자 테마 조회",
        description = "로그인한 사용자가 생성한 테마를 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my-themes")
    fun getUserThemes(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<List<ThemeResponse>> {
        val themes = themeService.getUserThemes(userPrincipal.id)
        return ResponseEntity.ok(themes)
    }

    @Operation(
        summary = "테마 검색",
        description = "키워드로 테마를 검색합니다."
    )
    @GetMapping("/search")
    fun searchThemes(
        @Parameter(description = "검색 키워드", required = true)
        @RequestParam
        keyword: String,

        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,

        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int
    ): ResponseEntity<Page<ThemeResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val themes = themeService.searchThemes(keyword, pageable)
        return ResponseEntity.ok(themes)
    }

    @Operation(
        summary = "테마 생성",
        description = "새로운 테마를 생성합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "테마 생성 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
            ApiResponse(responseCode = "401", description = "인증 필요")
        ]
    )
    @PostMapping
    fun createTheme(
        @RequestBody request: CreateThemeRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ThemeResponse> {
        return try {
            val theme = themeService.createTheme(request, userPrincipal.id)
            ResponseEntity.status(HttpStatus.CREATED).body(theme)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @Operation(
        summary = "테마 수정",
        description = "기존 테마를 수정합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "테마 수정 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "403", description = "권한 없음"),
            ApiResponse(responseCode = "404", description = "테마를 찾을 수 없음")
        ]
    )
    @PutMapping("/{themeId}")
    fun updateTheme(
        @Parameter(description = "테마 ID", required = true)
        @PathVariable
        themeId: Long,

        @RequestBody request: UpdateThemeRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ThemeResponse> {
        return try {
            val theme = themeService.updateTheme(themeId, request, userPrincipal.id)
            ResponseEntity.ok(theme)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @Operation(
        summary = "테마 삭제",
        description = "테마를 삭제(비활성화)합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "테마 삭제 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "403", description = "권한 없음"),
            ApiResponse(responseCode = "404", description = "테마를 찾을 수 없음")
        ]
    )
    @DeleteMapping("/{themeId}")
    fun deleteTheme(
        @Parameter(description = "테마 ID", required = true)
        @PathVariable
        themeId: Long,

        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Void> {
        return try {
            themeService.deleteTheme(themeId, userPrincipal.id)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: IllegalAccessException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @Operation(
        summary = "사이트에 테마 적용",
        description = "특정 사이트에 테마를 적용합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "테마 적용 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청"),
            ApiResponse(responseCode = "401", description = "인증 필요"),
            ApiResponse(responseCode = "404", description = "사이트 또는 테마를 찾을 수 없음")
        ]
    )
    @PostMapping("/sites/{siteId}/apply")
    fun applySiteTheme(
        @Parameter(description = "사이트 ID", required = true)
        @PathVariable
        siteId: Long,

        @RequestBody request: ApplySiteThemeRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val siteTheme = themeService.applySiteTheme(siteId, request.pageId, request, userPrincipal.id)
            val response = mapOf(
                "success" to true,
                "message" to "테마가 성공적으로 적용되었습니다.",
                "siteThemeId" to siteTheme.id,
                "appliedAt" to siteTheme.appliedAt
            )
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to (e.message ?: "요청 처리 중 오류가 발생했습니다.")
                )
            )
        }
    }

    @Operation(
        summary = "사이트의 기본 테마 조회",
        description = "특정 사이트에 적용된 기본 테마를 조회합니다."
    )
    @GetMapping("/sites/{siteId}/default")
    fun getSiteDefaultTheme(
        @Parameter(description = "사이트 ID", required = true)
        @PathVariable
        siteId: Long
    ): ResponseEntity<Map<String, Any?>> {
        val siteTheme = themeService.getSiteDefaultTheme(siteId)

        if (siteTheme == null) {
            return ResponseEntity.ok(
                mapOf(
                    "hasTheme" to false,
                    "message" to "적용된 기본 테마가 없습니다."
                )
            )
        }

        val theme = themeService.getThemeById(siteTheme.themeId)

        return ResponseEntity.ok(
            mapOf(
                "hasTheme" to true,
                "siteTheme" to siteTheme,
                "theme" to theme,
                "appliedAt" to siteTheme.appliedAt
            )
        )
    }

    @Operation(
        summary = "사이트의 페이지별 테마 조회",
        description = "특정 사이트의 특정 페이지에 적용된 테마를 조회합니다."
    )
    @GetMapping("/sites/{siteId}/page/{pageId}")
    fun getSitePageTheme(
        @Parameter(description = "사이트 ID", required = true)
        @PathVariable
        siteId: Long,

        @Parameter(description = "페이지 ID", required = true)
        @PathVariable
        pageId: String
    ): ResponseEntity<Map<String, Any?>> {
        val siteTheme = themeService.getSitePageTheme(siteId, pageId)

        if (siteTheme == null) {
            return ResponseEntity.ok(
                mapOf(
                    "hasTheme" to false,
                    "message" to "해당 페이지에 적용된 테마가 없습니다."
                )
            )
        }

        val theme = themeService.getThemeById(siteTheme.themeId)

        return ResponseEntity.ok(
            mapOf(
                "hasTheme" to true,
                "siteTheme" to siteTheme,
                "theme" to theme,
                "appliedAt" to siteTheme.appliedAt
            )
        )
    }

    @Operation(
        summary = "사이트의 모든 활성 테마 조회",
        description = "특정 사이트의 모든 활성 테마를 조회합니다."
    )
    @GetMapping("/sites/{siteId}/active")
    fun getSiteActiveThemes(
        @Parameter(description = "사이트 ID", required = true)
        @PathVariable
        siteId: Long
    ): ResponseEntity<List<Map<String, Any?>>> {
        val siteThemes = themeService.getSiteActiveThemes(siteId)

        val response = siteThemes.map { siteTheme ->
            val theme = themeService.getThemeById(siteTheme.themeId)
            mapOf(
                "siteTheme" to siteTheme,
                "theme" to theme,
                "appliedAt" to siteTheme.appliedAt
            )
        }

        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "사이트의 테마 변경 이력 조회",
        description = "특정 사이트의 테마 변경 이력을 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/sites/{siteId}/history")
    fun getSiteThemeHistory(
        @Parameter(description = "사이트 ID", required = true)
        @PathVariable
        siteId: Long,

        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<List<Map<String, Any?>>> {
        val history = themeService.getSiteThemeHistory(siteId)

        val response = history.map { siteTheme ->
            val theme = themeService.getThemeById(siteTheme.themeId)
            mapOf(
                "siteTheme" to siteTheme,
                "theme" to theme
            )
        }

        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "테마 커스터마이징 생성",
        description = "테마에 대한 커스터마이징을 생성합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/sites/{siteId}/themes/{themeId}/customizations")
    fun createThemeCustomization(
        @Parameter(description = "사이트 ID", required = true)
        @PathVariable
        siteId: Long,

        @Parameter(description = "테마 ID", required = true)
        @PathVariable
        themeId: Long,

        @RequestBody request: ThemeCustomizationRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val customization = themeService.createThemeCustomization(siteId, themeId, userPrincipal.id, request)
            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "success" to true,
                    "message" to "커스터마이징이 생성되었습니다.",
                    "customization" to customization
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "message" to (e.message ?: "요청 처리 중 오류가 발생했습니다.")
                )
            )
        }
    }

    @Operation(
        summary = "사이트의 커스터마이징 목록 조회",
        description = "특정 사이트의 모든 커스터마이징을 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/sites/{siteId}/customizations")
    fun getSiteCustomizations(
        @Parameter(description = "사이트 ID", required = true)
        @PathVariable
        siteId: Long,

        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<List<Map<String, Any?>>> {
        return try {
            val customizations = themeService.getSiteCustomizations(siteId, userPrincipal.id)

            val response = customizations.map { customization ->
                val theme = themeService.getThemeById(customization.themeId)
                mapOf(
                    "customization" to customization,
                    "theme" to theme
                )
            }

            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
}
