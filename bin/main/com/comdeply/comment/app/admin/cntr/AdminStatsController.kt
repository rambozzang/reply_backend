package com.comdeply.comment.app.admin.cntr

import com.comdeply.comment.app.admin.svc.*
import com.comdeply.comment.app.admin.svc.AdminService
import com.comdeply.comment.app.admin.svc.AdminStatsService
import com.comdeply.comment.app.admin.svc.vo.AdvancedStatsResponse
import com.comdeply.comment.app.admin.svc.vo.DetailedStatsResponse
import com.comdeply.comment.app.admin.svc.vo.PerformanceStatsResponse
import com.comdeply.comment.app.admin.svc.vo.TrendStatsResponse
import com.comdeply.comment.config.UserPrincipal
import com.comdeply.comment.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/admin/stats")
@CrossOrigin(originPatterns = ["*"])
@Tag(name = "관리자 - 통계", description = "관리자용 통계 API")
class AdminStatsController(
    private val adminService: AdminService,
    private val adminStatsService: AdminStatsService
) {
    private val logger = LoggerFactory.getLogger(AdminStatsController::class.java)

    @GetMapping("/detailed")
    @Operation(summary = "상세 통계 조회", description = "대시보드용 상세 통계를 조회합니다")
    fun getDetailedStats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<DetailedStatsResponse>> {
        logger.info("관리자 상세 통계 조회 요청")

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val detailedStats = adminStatsService.getDetailedStats(currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = detailedStats,
                    message = "상세 통계를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("상세 통계 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "상세 통계 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("상세 통계 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("통계 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/advanced")
    @Operation(summary = "고급 통계 조회", description = "관리자가 관리하는 사이트의 고급 통계를 조회합니다")
    fun getAdvancedStats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<AdvancedStatsResponse>> {
        logger.info("고급 통계 조회 요청")

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val advancedStats = adminStatsService.getAdvancedStats(currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = advancedStats,
                    message = "고급 통계를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("고급 통계 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "고급 통계 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("고급 통계 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("고급 통계 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/performance")
    @Operation(summary = "성능 통계 조회", description = "관리자가 관리하는 사이트의 성능 통계를 조회합니다")
    fun getPerformanceStats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<PerformanceStatsResponse>> {
        logger.info("성능 통계 조회 요청")

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val performanceStats = adminStatsService.getPerformanceStats(currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = performanceStats,
                    message = "성능 통계를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("성능 통계 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "성능 통계 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("성능 통계 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("성능 통계 조회에 실패했습니다")
            )
        }
    }

    @GetMapping("/trends")
    @Operation(summary = "트렌드 통계 조회", description = "관리자가 관리하는 사이트의 트렌드 통계를 조회합니다")
    fun getTrendStats(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): ResponseEntity<ApiResponse<TrendStatsResponse>> {
        logger.info("트렌드 통계 조회 요청")

        return try {
            val currentAdmin = adminService.findById(userPrincipal.id)
                ?: return ResponseEntity.status(401).body(
                    ApiResponse.error("관리자 정보를 찾을 수 없습니다")
                )

            val trendStats = adminStatsService.getTrendStats(currentAdmin)

            ResponseEntity.ok(
                ApiResponse.success(
                    data = trendStats,
                    message = "트렌드 통계를 성공적으로 조회했습니다"
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("트렌드 통계 조회 실패: {}", e.message)
            ResponseEntity.badRequest().body(
                ApiResponse.error(e.message ?: "트렌드 통계 조회에 실패했습니다")
            )
        } catch (e: Exception) {
            logger.error("트렌드 통계 조회 중 오류 발생", e)
            ResponseEntity.status(500).body(
                ApiResponse.error("트렌드 통계 조회에 실패했습니다")
            )
        }
    }
}
