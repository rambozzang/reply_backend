package com.comdeply.comment.app.admin.cntr

import com.comdeply.comment.app.admin.svc.AdminService
import com.comdeply.comment.app.admin.svc.AdminSubscriptionService
import com.comdeply.comment.app.admin.svc.vo.SubscriptionListResponse
import com.comdeply.comment.app.admin.svc.vo.SubscriptionStatsResponse
import com.comdeply.comment.app.admin.svc.vo.SubscriptionUpdateRequest
import com.comdeply.comment.common.PageResponse
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.ApiResponse
import com.comdeply.comment.dto.SubscriptionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/subscriptions")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "관리자 - 구독 관리", description = "관리자용 구독 관리 API")
class AdminSubscriptionController(
    private val adminService: AdminService,
    private val adminSubscriptionService: AdminSubscriptionService
) {
    private val logger = LoggerFactory.getLogger(AdminSubscriptionController::class.java)

    @GetMapping
    @Operation(summary = "구독 목록 조회", description = "전체 구독 목록을 조회합니다")
    fun getSubscriptions(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) planId: String?,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<ApiResponse<PageResponse<SubscriptionListResponse>>> {
        logger.info("관리자 구독 목록 조회 요청: page={}, size={}, status={}, planId={}", page, size, status, planId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val subscriptionsPage = adminSubscriptionService.getSubscriptions(
                admin = currentAdmin,
                page = page,
                size = size,
                status = status,
                planId = planId,
                search = search
            )
            val pageResponse = PageResponse.of(subscriptionsPage)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = pageResponse,
                    message = "구독 목록을 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("구독 목록 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "구독 목록 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("구독 목록 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("구독 목록 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/{subscriptionId}")
    @Operation(summary = "구독 상세 조회", description = "특정 구독의 상세 정보를 조회합니다")
    fun getSubscriptionDetail(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable subscriptionId: Long
    ): ResponseEntity<ApiResponse<SubscriptionResponse>> {
        logger.info("구독 상세 조회 요청: subscriptionId={}", subscriptionId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val subscription = adminSubscriptionService.getSubscriptionDetail(subscriptionId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = subscription,
                    message = "구독 상세 정보를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("구독 상세 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "구독을 찾을 수 없습니다")
            )
        } catch (e: Exception) {
            logger.error("구독 상세 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("구독 상세 조회에 실패했습니다")
            )
        }
    }

    @PutMapping("/{subscriptionId}")
    @Operation(summary = "구독 수정", description = "구독 정보를 수정합니다")
    fun updateSubscription(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable subscriptionId: Long,
        @Valid @RequestBody
        request: SubscriptionUpdateRequest
    ): ResponseEntity<ApiResponse<SubscriptionResponse>> {
        logger.info("구독 수정 요청: subscriptionId={}", subscriptionId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val updatedSubscription = adminSubscriptionService.updateSubscription(
                subscriptionId,
                request,
                currentAdmin
            )

            ResponseEntity.ok(
                ApiResponse.success(
                    data = updatedSubscription,
                    message = "구독이 성공적으로 수정되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("구독 수정 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "구독 수정에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("구독 수정 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("구독 수정에 실패했습니다")
            )
        }
    }

    @PostMapping("/{subscriptionId}/cancel")
    @Operation(summary = "구독 취소", description = "구독을 취소합니다")
    fun cancelSubscription(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable subscriptionId: Long,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("구독 취소 요청: subscriptionId={}", subscriptionId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val reason = request["reason"] as? String ?: "관리자에 의한 취소"

            adminSubscriptionService.cancelSubscription(subscriptionId, reason, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = "구독이 성공적으로 취소되었습니다",
                    message = "구독 취소가 완료되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("구독 취소 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "구독 취소에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("구독 취소 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("구독 취소에 실패했습니다")
            )
        }
    }

    @PostMapping("/{subscriptionId}/reactivate")
    @Operation(summary = "구독 재활성화", description = "취소된 구독을 재활성화합니다")
    fun reactivateSubscription(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable subscriptionId: Long
    ): ResponseEntity<ApiResponse<String>> {
        logger.info("구독 재활성화 요청: subscriptionId={}", subscriptionId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            adminSubscriptionService.reactivateSubscription(subscriptionId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = "구독이 성공적으로 재활성화되었습니다",
                    message = "구독 재활성화가 완료되었습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("구독 재활성화 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "구독 재활성화에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("구독 재활성화 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("구독 재활성화에 실패했습니다")
            )
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "구독 통계 조회", description = "구독 관련 통계를 조회합니다")
    fun getSubscriptionStats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?
    ): ResponseEntity<ApiResponse<SubscriptionStatsResponse>> {
        logger.info("구독 통계 조회 요청: startDate={}, endDate={}", startDate, endDate)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val stats = adminSubscriptionService.getSubscriptionStats(
                admin = currentAdmin,
                startDate = startDate,
                endDate = endDate
            )

            ResponseEntity.ok(
                ApiResponse.success(
                    data = stats,
                    message = "구독 통계를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("구독 통계 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "구독 통계 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("구독 통계 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("구독 통계 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자 구독 조회", description = "특정 사용자의 구독 정보를 조회합니다")
    fun getUserSubscription(
        @AuthenticationPrincipal userPrincipal: UserPrincipal,
        @PathVariable userId: Long
    ): ResponseEntity<ApiResponse<SubscriptionResponse?>> {
        logger.info("사용자 구독 조회 요청: userId={}", userId)

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val subscription = adminSubscriptionService.getUserSubscription(userId, currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = subscription,
                    message = "사용자 구독 정보를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("사용자 구독 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "사용자 구독 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("사용자 구독 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("사용자 구독 조회에 실패했습니다")
            )
        }
    }
}
