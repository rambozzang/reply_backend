package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.SiteService
import com.comdeply.comment.app.web.svc.ThemeService
import com.comdeply.comment.entity.PageType
import com.comdeply.comment.entity.SitePage
import com.comdeply.comment.repository.SitePageRepository
import jakarta.servlet.http.HttpServletRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "임베드", description = "위젯 임베드 및 설정 API")
@RestController
@RequestMapping("/embed")
class EmbedController(
    private val siteService: SiteService,
    private val themeService: ThemeService,
    private val sitePageRepository: SitePageRepository
) {

    @Value("\${widget.base-url}")
    private lateinit var widgetBaseUrl: String

    @Operation(
        summary = "임베드 스크립트 조회",
        description = "사이트에 삽입할 JavaScript 스크립트를 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "스크립트 반환 성공"),
            ApiResponse(responseCode = "404", description = "사이트를 찾을 수 없음")
        ]
    )
    @GetMapping("/script.js", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getEmbedScript(
        @Parameter(description = "사이트 ID (shortName)", required = true)
        @RequestParam
        siteKey: String
    ): ResponseEntity<String> {
        val site = siteService.findBySiteKey(siteKey)
            ?: return ResponseEntity.notFound().build()

        val script = generateEmbedScript(site.siteKey, site.domain)
        return ResponseEntity.ok()
            .header("Content-Type", "application/javascript")
            .body(script)
    }

    @Operation(
        summary = "위젯 설정 조회",
        description = "댓글 위젯의 설정 정보를 반환합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "설정 조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """
                        {
                            "siteId": "mysite",
                            "pageId": "/blog/post1",
                            "apiBaseUrl": "/api",
                            "websocketUrl": "/ws",
                            "theme": "light",
                            "language": "ko"
                        }
                        """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(responseCode = "404", description = "사이트를 찾을 수 없음")
        ]
    )
    @GetMapping("/config")
    fun getWidgetConfig(
        @Parameter(description = "사이트 키", required = true)
        @RequestParam
        siteKey: String,
        @Parameter(description = "페이지 ID", required = true)
        @RequestParam
        pageId: String,
        @Parameter(description = "페이지 제목", required = false)
        @RequestParam(required = false)
        pageTitle: String?,
        @Parameter(description = "페이지 URL", required = false)
        @RequestParam(required = false)
        pageUrl: String?,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        // 숫자인지 확인하여 ID로 조회할지 shortName으로 조회할지 결정
        val site =   siteService.findBySiteKey(siteKey)

        if (site == null) {
            return ResponseEntity<Map<String, Any>>(mapOf("error" to "SiteKey 정보가 없습니다."), org.springframework.http.HttpStatus.NOT_FOUND)
        }

        // 페이지 정보 자동 등록/업데이트
        autoRegisterPage(siteKey, pageId, pageTitle, pageUrl, request)

        // 페이지별 테마 정보 조회 (우선순위: 페이지별 테마 > 사이트 기본 테마)
        val siteTheme = themeService.getSitePageTheme(site.id, pageId) ?: themeService.getSiteDefaultTheme(site.id)
        val themeConfig = if (siteTheme != null) {
            try {
                val theme = themeService.getThemeById(siteTheme.themeId)
                mapOf(
                    "themeId" to theme.id,
                    "themeName" to theme.name,
                    "colors" to theme.colors,
                    "typography" to theme.typography,
                    "spacing" to theme.spacing,
                    "borderRadius" to theme.borderRadius,
                    "components" to theme.components,
                    "customCss" to theme.customCss,
                    "customizations" to siteTheme.customizations
                )
            } catch (e: Exception) {
                // 테마 조회 실패 시 기본값
                mapOf("themeName" to "default")
            }
        } else {
            // 적용된 테마가 없으면 기본 테마 사용
            mapOf("themeName" to "default")
        }

        val config = mapOf(
            "siteKey" to site.siteKey,
            "pageId" to pageId,
            "apiBaseUrl" to "/api",
            "websocketUrl" to "/ws",
            "theme" to site.theme, // 기존 호환성 유지
            "language" to site.language,
            "themeConfig" to themeConfig // 새로운 테마 설정
        )

        return ResponseEntity.ok(config)
    }

    @Operation(
        summary = "댓글 수 조회 스크립트",
        description = "외부 사이트에서 여러 페이지의 댓글 수를 조회할 수 있는 JavaScript를 반환합니다."
    )
    @GetMapping("/comment-counts.js", produces = ["application/javascript"])
    fun getCommentCountScript(): ResponseEntity<String> {
        val script = generateCommentCountScript()
        return ResponseEntity.ok()
            .header("Content-Type", "application/javascript")
            .header("Cache-Control", "public, max-age=3600")
            .body(script)
    }

    private fun generateEmbedScript(siteId: String, domain: String): String {
        return """
            (function() {
                var script = document.createElement('script');
                script.src = '$widgetBaseUrl/widget.js';
                script.dataset.siteId = '$siteId';
                script.dataset.domain = '$domain';
                script.dataset.pageId = window.location.pathname;
                document.head.appendChild(script);
            })();
        """.trimIndent()
    }

    private fun generateCommentCountScript(): String {
        return """
(function() {
    'use strict';
    
    // CommentCount 네임스페이스 생성
    window.ComdeplyCommentCount = window.ComdeplyCommentCount || {};
    
    var API_BASE_URL = '$widgetBaseUrl/api';
    
    /**
     * 단일 페이지의 댓글 수 조회
     * @param {Object} options - 옵션 객체
     * @param {string} options.siteId - 사이트 ID
     * @param {string} options.pageId - 페이지 ID
     * @param {Function} options.callback - 결과 콜백 함수
     */
    function getSingleCount(options) {
        if (!options.siteId || !options.pageId || !options.callback) {
            console.error('ComdeplyCommentCount: siteId, pageId, callback are required');
            return;
        }
        
        var url = API_BASE_URL + '/comments/count?siteId=' + 
                  encodeURIComponent(options.siteId) + '&pageId=' + 
                  encodeURIComponent(options.pageId);
        
        fetch(url)
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.json();
            })
            .then(function(data) {
                options.callback(data.count, null);
            })
            .catch(function(error) {
                console.error('ComdeplyCommentCount error:', error);
                options.callback(0, error);
            });
    }
    
    /**
     * 여러 페이지의 댓글 수를 배치 조회
     * @param {Object} options - 옵션 객체
     * @param {string} options.siteId - 사이트 ID
     * @param {Array<string>} options.pageIds - 페이지 ID 배열
     * @param {Function} options.callback - 결과 콜백 함수
     */
    function getBatchCounts(options) {
        if (!options.siteId || !options.pageIds || !Array.isArray(options.pageIds) || !options.callback) {
            console.error('ComdeplyCommentCount: siteId, pageIds (array), callback are required');
            return;
        }
        
        var url = API_BASE_URL + '/comments/count/batch';
        var requestBody = {
            siteId: parseInt(options.siteId),
            pageIds: options.pageIds
        };
        
        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestBody)
        })
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(function(data) {
            options.callback(data.counts, null);
        })
        .catch(function(error) {
            console.error('ComdeplyCommentCount error:', error);
            options.callback({}, error);
        });
    }
    
    /**
     * 페이지의 모든 댓글 수 엘리먼트를 자동으로 업데이트
     * @param {string} siteId - 사이트 ID
     */
    function updateAllCounts(siteId) {
        var elements = document.querySelectorAll('[data-comdeply-count]');
        if (elements.length === 0) return;
        
        var pageIds = [];
        var elementMap = {};
        
        // 모든 엘리먼트에서 pageId 수집
        elements.forEach(function(element) {
            var pageId = element.getAttribute('data-page-id') || window.location.pathname;
            pageIds.push(pageId);
            if (!elementMap[pageId]) {
                elementMap[pageId] = [];
            }
            elementMap[pageId].push(element);
        });
        
        // 중복 제거
        pageIds = Array.from(new Set(pageIds));
        
        // 배치 조회
        getBatchCounts({
            siteId: siteId,
            pageIds: pageIds,
            callback: function(counts, error) {
                if (error) {
                    console.error('Failed to load comment counts:', error);
                    return;
                }
                
                // 각 엘리먼트에 댓글 수 표시
                Object.keys(counts).forEach(function(pageId) {
                    var count = counts[pageId];
                    if (elementMap[pageId]) {
                        elementMap[pageId].forEach(function(element) {
                            element.textContent = count;
                            element.setAttribute('data-count', count);
                        });
                    }
                });
            }
        });
    }
    
    /**
     * 댓글 수 표시 엘리먼트 생성
     * @param {Object} options - 옵션 객체
     * @param {string} options.siteId - 사이트 ID
     * @param {string} options.pageId - 페이지 ID (선택사항, 기본값: 현재 경로)
     * @param {string} options.selector - 타겟 엘리먼트 선택자
     * @param {string} options.template - 표시 템플릿 (예: '{count}개의 댓글')
     */
    function renderCount(options) {
        if (!options.siteId || !options.selector) {
            console.error('ComdeplyCommentCount: siteId and selector are required');
            return;
        }
        
        var pageId = options.pageId || window.location.pathname;
        var template = options.template || '{count}';
        var element = document.querySelector(options.selector);
        
        if (!element) {
            console.error('ComdeplyCommentCount: Element not found:', options.selector);
            return;
        }
        
        getSingleCount({
            siteId: options.siteId,
            pageId: pageId,
            callback: function(count, error) {
                if (error) {
                    console.error('Failed to load comment count:', error);
                    element.textContent = template.replace('{count}', '0');
                    return;
                }
                
                element.textContent = template.replace('{count}', count);
                element.setAttribute('data-count', count);
            }
        });
    }
    
    // 공개 API
    window.ComdeplyCommentCount = {
        getSingleCount: getSingleCount,
        getBatchCounts: getBatchCounts,
        updateAllCounts: updateAllCounts,
        renderCount: renderCount
    };
    
    // DOM이 로드되면 자동으로 댓글 수 업데이트
    function init() {
        var siteId = document.querySelector('script[data-comdeply-site-id]');
        if (siteId) {
            updateAllCounts(siteId.getAttribute('data-comdeply-site-id'));
        }
    }
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
        """.trimIndent()
    }
    
    /**
     * 페이지 정보 자동 등록/업데이트
     */
    private fun autoRegisterPage(siteKey: String, pageId: String, pageTitle: String?, pageUrl: String?, request: HttpServletRequest) {
        try {
            val existingPage = sitePageRepository.findBySiteIdAndPageIdAndIsActiveTrue(siteKey, pageId)
            
            val currentTime = java.time.LocalDateTime.now()
            val actualPageUrl = pageUrl ?: request.getHeader("Referer")
            val userAgent = request.getHeader("User-Agent")
            
            // 페이지 타입 추론
            val pageType = inferPageType(pageId, pageTitle, actualPageUrl)
            
            if (existingPage != null) {
                // 기존 페이지 업데이트
                var updated = false
                
                // 페이지 제목이 있고 기존 제목이 없거나 다른 경우 업데이트
                if (!pageTitle.isNullOrBlank() && existingPage.pageName != pageTitle) {
                    existingPage.pageName = pageTitle
                    updated = true
                }
                
                // 페이지 URL 업데이트
                if (!actualPageUrl.isNullOrBlank() && existingPage.pageUrl != actualPageUrl) {
                    existingPage.pageUrl = actualPageUrl
                    updated = true
                }
                
                // 페이지 타입 업데이트 (더 구체적인 타입으로만 업데이트)
                if (pageType != PageType.GENERAL && existingPage.pageType == PageType.GENERAL) {
                    existingPage.pageType = pageType
                    updated = true
                }
                
                // 마지막 활동 시간 업데이트 (항상)
                existingPage.lastActivityAt = currentTime
                existingPage.updatedAt = currentTime
                updated = true
                
                if (updated) {
                    sitePageRepository.save(existingPage)
                }
            } else {
                // 새 페이지 등록
                val newPage = SitePage(
                    siteId = siteKey,
                    pageId = pageId,
                    pageName = pageTitle?.take(255), // 길이 제한
                    pageDescription = generatePageDescription(pageType, pageTitle),
                    pageUrl = actualPageUrl?.take(500), // 길이 제한
                    pageType = pageType,
                    lastActivityAt = currentTime
                )
                
                sitePageRepository.save(newPage)
                
                // 로그 출력 (개발/디버깅용)
                println("📝 새 페이지 자동 등록: $siteKey/$pageId (${pageTitle ?: "제목 없음"})")
            }
        } catch (e: Exception) {
            // 페이지 등록 실패해도 댓글 위젯 로드는 정상 진행
            println("⚠️ 페이지 자동 등록 실패: $siteKey/$pageId - ${e.message}")
        }
    }
    
    /**
     * 페이지 ID, 제목, URL을 기반으로 페이지 타입 추론
     */
    private fun inferPageType(pageId: String, pageTitle: String?, pageUrl: String?): PageType {
        val lowerPageId = pageId.lowercase()
        val lowerTitle = pageTitle?.lowercase() ?: ""
        val lowerUrl = pageUrl?.lowercase() ?: ""
        
        return when {
            // 게시판 관련
            lowerPageId.contains("board") || lowerPageId.contains("forum") || 
            lowerPageId.contains("community") || lowerPageId.contains("notice") ||
            lowerTitle.contains("게시판") || lowerTitle.contains("board") ||
            lowerTitle.contains("커뮤니티") || lowerTitle.contains("공지") -> PageType.BOARD
            
            // 상품 관련
            lowerPageId.contains("product") || lowerPageId.contains("item") ||
            lowerPageId.contains("shop") || lowerPageId.contains("store") ||
            lowerTitle.contains("상품") || lowerTitle.contains("product") ||
            lowerUrl.contains("/product/") || lowerUrl.contains("/item/") -> PageType.PRODUCT
            
            // 개별 글/기사
            lowerPageId.contains("post") || lowerPageId.contains("article") ||
            lowerPageId.contains("blog") || lowerPageId.contains("news") ||
            lowerUrl.contains("/post/") || lowerUrl.contains("/article/") ||
            lowerUrl.contains("/blog/") -> PageType.ARTICLE
            
            // 기본값
            else -> PageType.GENERAL
        }
    }
    
    /**
     * 페이지 타입에 따른 기본 설명 생성
     */
    private fun generatePageDescription(pageType: PageType, pageTitle: String?): String? {
        if (pageTitle.isNullOrBlank()) return null
        
        return when (pageType) {
            PageType.BOARD -> "$pageTitle - 게시판"
            PageType.PRODUCT -> "$pageTitle - 상품 페이지"
            PageType.ARTICLE -> "$pageTitle - 글"
            PageType.GENERAL -> pageTitle
            PageType.OTHER -> pageTitle
        }
    }
}
