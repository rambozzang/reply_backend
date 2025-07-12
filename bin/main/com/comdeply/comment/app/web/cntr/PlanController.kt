package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.PlanValidationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "플랜", description = "사용자 플랜 및 제한 조회 API")
@RestController
@RequestMapping("/api/plan")
class PlanController(
    private val planValidationService: PlanValidationService
) {

    @Operation(
        summary = "사용자 플랜 정보 조회",
        description = "현재 사용자의 플랜 정보, 제한, 사용량을 조회합니다."
    )
    @GetMapping("/info")
    fun getPlanInfo(request: HttpServletRequest): ResponseEntity<PlanValidationService.PlanInfo> {
        val userId = request.getAttribute("userId") as Long
        val planInfo = planValidationService.getUserPlanInfo(userId)
        return ResponseEntity.ok(planInfo)
    }

    @Operation(
        summary = "사이트 생성 가능 여부 확인",
        description = "현재 플랜에서 사이트를 추가로 생성할 수 있는지 확인합니다."
    )
    @GetMapping("/check/site")
    fun checkSiteCreation(request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val userId = request.getAttribute("userId") as Long
        val validation = planValidationService.canCreateSite(userId)

        return ResponseEntity.ok(
            mapOf(
                "canCreate" to validation.isValid,
                "message" to validation.message
            )
        )
    }

    @Operation(
        summary = "도메인 추가 가능 여부 확인",
        description = "현재 플랜에서 지정된 수의 도메인을 추가할 수 있는지 확인합니다."
    )
    @GetMapping("/check/domains")
    fun checkDomainAddition(
        @RequestParam currentCount: Int,
        @RequestParam additionalCount: Int,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> {
        val userId = request.getAttribute("userId") as Long
        val validation = planValidationService.canAddDomains(userId, currentCount, additionalCount)

        return ResponseEntity.ok(
            mapOf(
                "canAdd" to validation.isValid,
                "message" to validation.message
            )
        )
    }
}
