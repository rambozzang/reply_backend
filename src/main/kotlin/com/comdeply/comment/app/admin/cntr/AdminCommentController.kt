package com.comdeply.comment.app.admin.cntr

import com.comdeply.comment.app.admin.svc.AdminCommentService
import com.comdeply.comment.app.admin.svc.AdminService
import com.comdeply.comment.app.admin.svc.vo.CommentStatsResponse
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.ApiResponse
import com.comdeply.comment.dto.CommentListResponse
import com.comdeply.comment.dto.CommentResponse
import com.comdeply.comment.entity.CommentStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/comments")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "관리자 - 댓글 관리", description = "관리자용 댓글 관리 API")
class AdminCommentController(
    private val adminService: AdminService,
    private val adminCommentService: AdminCommentService
) {
    private val logger = LoggerFactory.getLogger(AdminCommentController::class.java)

    @GetMapping
    @Operation(summary = "관리자용 댓글 목록 조회", description = "사이트별, 페이지별 댓글을 조회합니다")
    fun getCommentsForAdmin(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) siteId: Long?,
        @RequestParam(required = false) pageId: String?,
        @RequestParam(required = false) status: CommentStatus?,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<ApiResponse<CommentListResponse>> {
        logger.info("관리자 댓글 목록 조회 요청: page={}, size={}, siteId={}, pageId={}", page, size, siteId, pageId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val comments = adminCommentService.getCommentsForAdmin(
                admin = currentAdmin,
                siteId = siteId,
                pageId = pageId,
                status = status,
                search = search,
                page = page,
                size = size
            )

            ResponseEntity.ok(
                ApiResponse.success(
                    data = comments,
                    message = "댓글 목록을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("댓글 목록 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "댓글 목록 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("댓글 목록 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("댓글 목록 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "댓글 상세 조회", description = "특정 댓글의 상세 정보를 조회합니다")
    fun getCommentDetail(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable commentId: Long
    ): ResponseEntity<ApiResponse<CommentResponse>> {
        logger.info("댓글 상세 조회 요청: commentId={}", commentId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val comment = adminCommentService.getCommentDetail(commentId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = comment,
                    message = "댓글 정보를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("댓글 상세 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "댓글 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("댓글 상세 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("댓글 조회에 실패했습니다")
            )
        }
    }

    @PutMapping("/{commentId}/status")
    @Operation(summary = "댓글 상태 변경", description = "댓글의 승인 상태를 변경합니다")
    fun updateCommentStatus(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable commentId: Long,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("댓글 상태 변경 요청: commentId={}", commentId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val statusString = request["status"] as? String
                ?: return ResponseEntity.badRequest().body(
                    ApiResponse.error("상태 정보가 필요합니다")
                )

            val newStatus = try {
                CommentStatus.valueOf(statusString.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("올바르지 않은 상태값입니다")
                )
            }

            val reason = request["reason"] as? String

            adminCommentService.updateCommentStatus(commentId, newStatus, reason, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = "댓글 상태가 성공적으로 변경되었습니다",
                    message = "댓글 상태 변경이 완료되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("댓글 상태 변경 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "댓글 상태 변경에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("댓글 상태 변경 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("댓글 상태 변경에 실패했습니다")
            )
        }
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다")
    fun deleteComment(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable commentId: Long,
        @RequestBody(required = false) request: Map<String, Any>?
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("댓글 삭제 요청: commentId={}", commentId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            adminCommentService.deleteCommentByAdmin(commentId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = "댓글이 성공적으로 삭제되었습니다",
                    message = "댓글 삭제가 완료되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("댓글 삭제 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "댓글 삭제에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("댓글 삭제 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("댓글 삭제에 실패했습니다")
            )
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "댓글 통계 조회", description = "댓글 상태별 통계를 조회합니다")
    fun getCommentStats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(required = false) siteId: Long?
    ): ResponseEntity<ApiResponse<CommentStatsResponse>> {
        logger.info("댓글 통계 조회 요청: siteId={}", siteId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val stats = adminCommentService.getCommentStats(currentAdmin, siteId)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = stats,
                    message = "댓글 통계를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("댓글 통계 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "댓글 통계 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("댓글 통계 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("댓글 통계 조회에 실패했습니다")
            )
        }
    }
}
