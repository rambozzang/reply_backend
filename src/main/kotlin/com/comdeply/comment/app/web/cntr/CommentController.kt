package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.CommentService
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "댓글 관리", description = "댓글 CRUD 및 좋아요/싫어요 API")
@RestController
@RequestMapping("/comments")
class CommentController(
    private val commentService: CommentService
) {

    @Operation(summary = "댓글 목록 조회", description = "특정 사이트의 페이지에 대한 댓글 목록을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터")
        ]
    )
    @GetMapping
    fun getComments(
        @Parameter(description = "사이트 키", required = true)
        @RequestParam
        siteKey: String,
        @Parameter(description = "페이지 ID", required = true)
        @RequestParam
        pageId: String,
        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int,
        @Parameter(description = "트리 구조 반환 여부", example = "true")
        @RequestParam(defaultValue = "true")
        hierarchy: Boolean,
        @AuthenticationPrincipal userPrincipal: UserPrincipal?
    ): ResponseEntity<CommentListResponse> {
        val response = if (hierarchy) {
            commentService.getCommentsHierarchy(siteKey, pageId, page, size, userPrincipal?.id)
        } else {
            commentService.getComments(siteKey, pageId, page, size, userPrincipal?.id)
        }
        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun createComment(
        @Valid @RequestBody
        request: CommentCreateRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        httpRequest: HttpServletRequest
    ): ResponseEntity<CommentResponse> {
        val ipAddress = getClientIpAddress(httpRequest)
        val response = commentService.createComment(request, userPrincipal.id, ipAddress)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{commentId}")
    fun updateComment(
        @PathVariable commentId: Long,
        @Valid @RequestBody
        request: CommentUpdateRequest,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CommentResponse> {
        val response = commentService.updateComment(commentId, request, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Void> {
        commentService.deleteComment(commentId, userPrincipal.id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{commentId}/like")
    fun likeComment(
        @PathVariable commentId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CommentResponse> {
        val response = commentService.likeComment(commentId, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{commentId}/dislike")
    fun dislikeComment(
        @PathVariable commentId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<CommentResponse> {
        val response = commentService.dislikeComment(commentId, userPrincipal.id)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/reorder")
    fun reorderComments(
        @RequestParam siteKey: String,
        @RequestParam pageId: String,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<Void> {
        commentService.reorderAllComments(siteKey, pageId)
        return ResponseEntity.ok().build()
    }

    @Operation(summary = "댓글 수 조회", description = "특정 사이트의 페이지에 대한 댓글 수를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "댓글 수 조회 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터")
        ]
    )
    @GetMapping("/count")
    fun getCommentCount(
        @Parameter(description = "사이트 키", required = true)
        @RequestParam
        siteKey: String,
        @Parameter(description = "페이지 ID", required = true)
        @RequestParam
        pageId: String
    ): ResponseEntity<CommentCountResponse> {
        val count = commentService.getCommentCount(siteKey, pageId)
        return ResponseEntity.ok(CommentCountResponse(count))
    }

    @Operation(summary = "배치 댓글 수 조회", description = "여러 페이지의 댓글 수를 한 번에 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "배치 댓글 수 조회 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터")
        ]
    )
    @PostMapping("/count/batch")
    fun getBatchCommentCount(
        @Valid @RequestBody
        request: BatchCommentCountRequest
    ): ResponseEntity<BatchCommentCountResponse> {
        val counts = commentService.getBatchCommentCount(request.siteKey, request.pageIds)
        return ResponseEntity.ok(BatchCommentCountResponse(counts))
    }

    /**
     * 클라이언트의 실제 IP 주소를 추출합니다.
     * 프록시나 로드밸런서를 고려하여 여러 헤더를 확인합니다.
     */
    private fun getClientIpAddress(request: HttpServletRequest): String {
        val headers = listOf(
            "X-Forwarded-For",
            "X-Real-IP", "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
        )

        for (header in headers) {
            val ip = request.getHeader(header)
            if (!ip.isNullOrBlank() && !"unknown".equals(ip, ignoreCase = true)) {
                // X-Forwarded-For는 여러 IP가 쉼표로 구분될 수 있음
                return ip.split(",").first().trim()
            }
        }

        // 모든 헤더에서 IP를 찾지 못했을 경우 기본 remote address 사용
        return request.remoteAddr ?: "unknown"
    }
}
