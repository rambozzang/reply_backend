package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.SubscriptionService
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.ApiResponse
import com.comdeply.comment.dto.SubscriptionCancelRequest
import com.comdeply.comment.dto.SubscriptionResponse
import com.comdeply.comment.repository.AdminRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "구독 API", description = "사용자용 구독 관련 API")
class SubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val adminRepository: AdminRepository
) {

    private val logger = LoggerFactory.getLogger(SubscriptionController::class.java)

    @Operation(summary = "구독 정보 조회", description = "사용자의 현재 구독 정보를 조회합니다")
    @GetMapping("/current")
    fun getCurrentSubscription(
        @Parameter(description = "인증된 사용자 정보", hidden = true)
        @AuthenticationPrincipal
        userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<SubscriptionResponse?>> {
        return try {
            val subscription = subscriptionService.getAdminSubscription(userPrincipal.id)

            logger.info("구독 정보 조회 성공: 관리자=${userPrincipal.id}, 구독=${subscription?.id}")

            ResponseEntity.ok(ApiResponse.success(subscription))
        } catch (e: Exception) {
            logger.error("구독 정보 조회 실패: 사용자=${userPrincipal.id}", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<SubscriptionResponse?>("구독 정보를 조회할 수 없습니다: ${e.message}")
            )
        }
    }

    @Operation(summary = "구독 취소", description = "현재 구독을 취소합니다")
    @PostMapping("/{subscriptionId}/cancel")
    fun cancelSubscription(
        @Parameter(description = "인증된 사용자 정보", hidden = true)
        @AuthenticationPrincipal
        userPrincipal: UserPrincipal,
        @Parameter(description = "구독 ID")
        @PathVariable
        subscriptionId: Long,
        @RequestBody @Valid
        request: SubscriptionCancelRequest
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return try {
            val result = subscriptionService.cancelSubscription(subscriptionId, request)

            logger.info("구독 취소 처리: 사용자=${userPrincipal.id}, 구독ID=$subscriptionId, 성공=$result")

            if (result) {
                ResponseEntity.ok(
                    ApiResponse.success(
                        mapOf(
                            "success" to true,
                            "message" to "구독이 취소되었습니다"
                        )
                    )
                )
            } else {
                ResponseEntity.badRequest().body(
                    ApiResponse.error<Map<String, Any>>("구독 취소에 실패했습니다")
                )
            }
        } catch (e: Exception) {
            logger.error("구독 취소 실패: 사용자=${userPrincipal.id}, 구독ID=$subscriptionId", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<Map<String, Any>>("구독 취소에 실패했습니다: ${e.message}")
            )
        }
    }

    @Operation(summary = "댓글 한도 확인", description = "현재 댓글 한도를 확인합니다")
    @GetMapping("/comment-limit")
    fun checkCommentLimit(
        @Parameter(description = "인증된 사용자 정보", hidden = true)
        @AuthenticationPrincipal
        userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return try {
            val hasLimit = subscriptionService.checkCommentLimit(userPrincipal.id)
            val subscription = subscriptionService.getAdminSubscription(userPrincipal.id)

            val response = mapOf(
                "hasLimit" to hasLimit,
                "currentCount" to (subscription?.currentCommentCount ?: 0),
                "monthlyLimit" to (subscription?.monthlyCommentLimit ?: 0),
                "planName" to (subscription?.planName ?: "Free")
            )

            logger.info("댓글 한도 확인: 사용자=${userPrincipal.id}, 한도=$hasLimit")

            ResponseEntity.ok(ApiResponse.success(response))
        } catch (e: Exception) {
            logger.error("댓글 한도 확인 실패: 사용자=${userPrincipal.id}", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<Map<String, Any>>("댓글 한도를 확인할 수 없습니다: ${e.message}")
            )
        }
    }

    @Operation(summary = "구독 생성", description = "새로운 구독을 생성합니다 (온보딩용)")
    @PostMapping
    fun createSubscription(
        @Parameter(description = "인증된 사용자 정보", hidden = true)
        @AuthenticationPrincipal
        userPrincipal: UserPrincipal,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<SubscriptionResponse>> {
        return try {
            val planId = request["planId"] as? String
                ?: return ResponseEntity.badRequest().body(
                    ApiResponse.error("플랜 ID가 필요합니다")
                )

            val planName = request["planName"] as? String
                ?: return ResponseEntity.badRequest().body(
                    ApiResponse.error("플랜 이름이 필요합니다")
                )

            val amount = (request["amount"] as? Number)?.toInt()
                ?: return ResponseEntity.badRequest().body(
                    ApiResponse.error("결제 금액이 필요합니다")
                )

            val admin = adminRepository.findById(userPrincipal.id).orElse(null)
                ?: return ResponseEntity.badRequest().body(
                    ApiResponse.error("관리자를 찾을 수 없습니다")
                )

            val subscription = subscriptionService.createSubscription(admin, planId, planName, amount)

            val subscriptionResponse = SubscriptionResponse(
                id = subscription.id,
                userId = subscription.admin.id,
                planId = subscription.planId,
                planName = subscription.planName,
                price = subscription.amount,
                monthlyCommentLimit = subscription.monthlyCommentLimit,
                currentCommentCount = subscription.currentCommentCount,
                status = subscription.status.name.lowercase(),
                startDate = subscription.startDate.toString(),
                nextBillingDate = subscription.nextBillingDate?.toString(),
                cancelledAt = subscription.cancelledAt?.toString(),
                cancelReason = subscription.cancelReason,
                createdAt = subscription.createdAt.toString(),
                updatedAt = subscription.updatedAt.toString()
            )

            logger.info("구독 생성 성공: 사용자=${userPrincipal.id}, 플랜=$planId")

            ResponseEntity.ok(
                ApiResponse.success(
                    data = subscriptionResponse,
                    message = "구독이 성공적으로 생성되었습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("구독 생성 실패: 사용자=${userPrincipal.id}", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<SubscriptionResponse>("구독 생성에 실패했습니다: ${e.message}")
            )
        }
    }

    @Operation(summary = "구독 업그레이드", description = "현재 구독을 업그레이드합니다")
    @PostMapping("/upgrade")
    fun upgradeSubscription(
        @Parameter(description = "인증된 사용자 정보", hidden = true)
        @AuthenticationPrincipal
        userPrincipal: UserPrincipal,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<SubscriptionResponse>> {
        return try {
            val planId = request["planId"] as? String
                ?: return ResponseEntity.badRequest().body(
                    ApiResponse.error("플랜 ID가 필요합니다")
                )

            val subscription = subscriptionService.upgradeSubscription(userPrincipal.id, planId)

            logger.info("구독 업그레이드 성공: 사용자=${userPrincipal.id}, 플랜=$planId")

            ResponseEntity.ok(
                ApiResponse.success(
                    data = subscription,
                    message = "구독이 성공적으로 업그레이드되었습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("구독 업그레이드 실패: 사용자=${userPrincipal.id}", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<SubscriptionResponse>("구독 업그레이드에 실패했습니다: ${e.message}")
            )
        }
    }

    @Operation(summary = "구독 다운그레이드", description = "현재 구독을 다운그레이드합니다")
    @PostMapping("/downgrade")
    fun downgradeSubscription(
        @Parameter(description = "인증된 사용자 정보", hidden = true)
        @AuthenticationPrincipal
        userPrincipal: UserPrincipal,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<ApiResponse<SubscriptionResponse>> {
        return try {
            val planId = request["planId"] as? String
                ?: return ResponseEntity.badRequest().body(
                    ApiResponse.error("플랜 ID가 필요합니다")
                )

            val subscription = subscriptionService.downgradeSubscription(userPrincipal.id, planId)

            logger.info("구독 다운그레이드 성공: 사용자=${userPrincipal.id}, 플랜=$planId")

            ResponseEntity.ok(
                ApiResponse.success(
                    data = subscription,
                    message = "구독이 성공적으로 다운그레이드되었습니다"
                )
            )
        } catch (e: Exception) {
            logger.error("구독 다운그레이드 실패: 사용자=${userPrincipal.id}", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<SubscriptionResponse>("구독 다운그레이드에 실패했습니다: ${e.message}")
            )
        }
    }

    @Operation(summary = "플랜 정보 조회", description = "사용 가능한 플랜 정보를 조회합니다")
    @GetMapping("/plans")
    fun getAvailablePlans(): ResponseEntity<ApiResponse<List<Map<String, Any>>>> {
        return try {
            val starterLimits = com.comdeply.comment.utils.PlanLimits.getByPlan(com.comdeply.comment.utils.PlanType.STARTER)
            val proLimits = com.comdeply.comment.utils.PlanLimits.getByPlan(com.comdeply.comment.utils.PlanType.PRO)
            val enterpriseLimits = com.comdeply.comment.utils.PlanLimits.getByPlan(com.comdeply.comment.utils.PlanType.ENTERPRISE)
            
            val plans = listOf(
                mapOf(
                    "id" to "starter",
                    "name" to "Starter 플랜",
                    "price" to 0,
                    "monthlyCommentLimit" to starterLimits.monthlyCommentLimit,
                    "maxSites" to starterLimits.maxSites,
                    "features" to listOf(
                        "월 ${starterLimits.monthlyCommentLimit}개 댓글",
                        "최대 ${starterLimits.maxSites}개 사이트",
                        "기본 테마",
                        "이메일 지원"
                    )
                ),
                mapOf(
                    "id" to "pro",
                    "name" to "Pro 플랜",
                    "price" to 29000,
                    "monthlyCommentLimit" to proLimits.monthlyCommentLimit,
                    "maxSites" to proLimits.maxSites,
                    "features" to listOf(
                        "월 ${proLimits.monthlyCommentLimit}개 댓글",
                        "최대 ${proLimits.maxSites}개 사이트",
                        "기본 테마 제공",
                        "이메일 지원",
                        "기본 통계"
                    )
                ),
                mapOf(
                    "id" to "enterprise",
                    "name" to "Enterprise 플랜",
                    "price" to 99000,
                    "monthlyCommentLimit" to enterpriseLimits.monthlyCommentLimit,
                    "maxSites" to enterpriseLimits.maxSites,
                    "features" to listOf(
                        "월 ${if (enterpriseLimits.monthlyCommentLimit == -1) "무제한" else enterpriseLimits.monthlyCommentLimit}개 댓글",
                        "${if (enterpriseLimits.maxSites == -1) "무제한" else "최대 ${enterpriseLimits.maxSites}개"} 사이트",
                        "커스텀 테마 제공",
                        "우선 지원",
                        "고급 통계",
                        "API 액세스",
                        "전용 지원팀"
                    )
                )
            )

            ResponseEntity.ok(ApiResponse.success(plans))
        } catch (e: Exception) {
            logger.error("플랜 정보 조회 실패", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<List<Map<String, Any>>>("플랜 정보를 조회할 수 없습니다: ${e.message}")
            )
        }
    }
}
