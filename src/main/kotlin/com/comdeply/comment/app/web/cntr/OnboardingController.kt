package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.OnboardingService
import com.comdeply.comment.dto.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/onboarding")
class OnboardingController(
    private val onboardingService: OnboardingService
) {

    /**
     * 1단계: 사용자 정보 등록 (관리자 계정 생성)
     */
    @PostMapping("/step1/admin")
    fun registerAdmin(
        @Valid @RequestBody
        request: OnboardingAdminRegisterRequest
    ): ResponseEntity<OnboardingResponse> {
        return try {
            val result = onboardingService.registerAdmin(request)
            ResponseEntity.ok(
                OnboardingResponse(
                    success = true,
                    message = "관리자 계정이 성공적으로 생성되었습니다.",
                    data = result
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                OnboardingResponse(
                    success = false,
                    message = e.message ?: "관리자 등록에 실패했습니다.",
                    data = null
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                OnboardingResponse(
                    success = false,
                    message = "서버 오류가 발생했습니다.",
                    data = null
                )
            )
        }
    }

    /**
     * 1단계: 로그인
     */
    @PostMapping("/step1/login")
    fun login(
        @Valid @RequestBody
        request: OnboardingAdminLoginRequest
    ): ResponseEntity<OnboardingResponse> {
        return try {
            val result = onboardingService.loginAdmin(request)
            ResponseEntity.ok(
                OnboardingResponse(
                    success = true,
                    message = "로그인에 성공했습니다.",
                    data = result
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                OnboardingResponse(
                    success = false,
                    message = e.message ?: "로그인에 실패했습니다.",
                    data = null
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                OnboardingResponse(
                    success = false,
                    message = "서버 오류가 발생했습니다.",
                    data = null
                )
            )
        }
    }

    /**
     * 2단계: 사이트 정보 등록
     */
    @PostMapping("/step2/site")
    fun registerSite(
        @Valid @RequestBody
        request: OnboardingSiteRegisterRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<OnboardingResponse> {
        return try {
            val result = onboardingService.registerSite(request, token)
            ResponseEntity.ok(
                OnboardingResponse(
                    success = true,
                    message = "사이트가 성공적으로 등록되었습니다.",
                    data = result
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                OnboardingResponse(
                    success = false,
                    message = e.message ?: "사이트 등록에 실패했습니다.",
                    data = null
                )
            )
        } catch (e: SecurityException) {
            ResponseEntity.status(401).body(
                OnboardingResponse(
                    success = false,
                    message = "인증이 필요합니다.",
                    data = null
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                OnboardingResponse(
                    success = false,
                    message = "서버 오류가 발생했습니다.",
                    data = null
                )
            )
        }
    }

    /**
     * 2단계: 스킨 적용
     */
    @PostMapping("/step2/skin")
    fun applySkin(
        @Valid @RequestBody
        request: OnboardingSkinApplyRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<OnboardingResponse> {
        return try {
            val result = onboardingService.applySkin(request, token)
            ResponseEntity.ok(
                OnboardingResponse(
                    success = true,
                    message = "스킨이 성공적으로 적용되었습니다.",
                    data = result
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                OnboardingResponse(
                    success = false,
                    message = e.message ?: "스킨 적용에 실패했습니다.",
                    data = null
                )
            )
        } catch (e: SecurityException) {
            ResponseEntity.status(401).body(
                OnboardingResponse(
                    success = false,
                    message = "인증이 필요합니다.",
                    data = null
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                OnboardingResponse(
                    success = false,
                    message = "서버 오류가 발생했습니다.",
                    data = null
                )
            )
        }
    }

    /**
     * 3단계: 구독 생성 (무료 플랜)
     */
    @PostMapping("/step3/subscription/free")
    fun createFreeSubscription(
        @Valid @RequestBody
        request: OnboardingFreeSubscriptionRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<OnboardingResponse> {
        return try {
            val result = onboardingService.createFreeSubscription(request, token)
            ResponseEntity.ok(
                OnboardingResponse(
                    success = true,
                    message = "무료 구독이 성공적으로 생성되었습니다.",
                    data = result
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                OnboardingResponse(
                    success = false,
                    message = e.message ?: "구독 생성에 실패했습니다.",
                    data = null
                )
            )
        } catch (e: SecurityException) {
            ResponseEntity.status(401).body(
                OnboardingResponse(
                    success = false,
                    message = "인증이 필요합니다.",
                    data = null
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                OnboardingResponse(
                    success = false,
                    message = "서버 오류가 발생했습니다.",
                    data = null
                )
            )
        }
    }

    /**
     * 3단계: 결제 및 구독 생성 (유료 플랜)
     */
    @PostMapping("/step3/subscription/paid")
    fun createPaidSubscription(
        @Valid @RequestBody
        request: OnboardingPaidSubscriptionRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<OnboardingResponse> {
        return try {
            val result = onboardingService.createPaidSubscription(request, token)
            ResponseEntity.ok(
                OnboardingResponse(
                    success = true,
                    message = "결제 및 구독이 성공적으로 처리되었습니다.",
                    data = result
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                OnboardingResponse(
                    success = false,
                    message = e.message ?: "결제 처리에 실패했습니다.",
                    data = null
                )
            )
        } catch (e: SecurityException) {
            ResponseEntity.status(401).body(
                OnboardingResponse(
                    success = false,
                    message = "인증이 필요합니다.",
                    data = null
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                OnboardingResponse(
                    success = false,
                    message = "서버 오류가 발생했습니다.",
                    data = null
                )
            )
        }
    }

    /**
     * 4단계: 완료 정보 조회 (임베드 코드 생성)
     */
    @GetMapping("/step4/complete")
    fun getCompletionInfo(
        @RequestParam siteKey: String,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<OnboardingResponse> {
        return try {
            val result = onboardingService.getCompletionInfo(siteKey, token)
            ResponseEntity.ok(
                OnboardingResponse(
                    success = true,
                    message = "완료 정보를 성공적으로 조회했습니다.",
                    data = result
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                OnboardingResponse(
                    success = false,
                    message = e.message ?: "완료 정보 조회에 실패했습니다.",
                    data = null
                )
            )
        } catch (e: SecurityException) {
            ResponseEntity.status(401).body(
                OnboardingResponse(
                    success = false,
                    message = "인증이 필요합니다.",
                    data = null
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                OnboardingResponse(
                    success = false,
                    message = "서버 오류가 발생했습니다.",
                    data = null
                )
            )
        }
    }

    /**
     * 이메일 중복 체크
     */
    @GetMapping("/check-email")
    fun checkEmailExists(@RequestParam email: String): ResponseEntity<Map<String, Boolean>> {
        val exists = onboardingService.checkEmailExists(email)
        return ResponseEntity.ok(mapOf("exists" to exists))
    }

    /**
     * 도메인 중복 체크
     */
    @GetMapping("/check-domain")
    fun checkDomainExists(@RequestParam domain: String): ResponseEntity<Map<String, Boolean>> {
        val exists = onboardingService.checkDomainExists(domain)
        return ResponseEntity.ok(mapOf("exists" to exists))
    }

    /**
     * 사용자명 중복 체크
     */
    @GetMapping("/check-username")
    fun checkUsernameExists(@RequestParam username: String): ResponseEntity<Map<String, Boolean>> {
        val exists = onboardingService.checkUsernameExists(username)
        return ResponseEntity.ok(mapOf("exists" to exists))
    }

    /**
     * 사용 가능한 테마 목록 조회
     */
    @GetMapping("/themes")
    fun getAvailableThemes(@RequestParam planId: String): ResponseEntity<OnboardingResponse> {
        return try {
            val themes = onboardingService.getAvailableThemes(planId)
            ResponseEntity.ok(
                OnboardingResponse(
                    success = true,
                    message = "테마 목록을 성공적으로 조회했습니다.",
                    data = themes
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                OnboardingResponse(
                    success = false,
                    message = "테마 목록 조회에 실패했습니다.",
                    data = null
                )
            )
        }
    }

    /**
     * 2단계: 사이트 정보 및 스킨 통합 등록
     */
    @PostMapping("/step2/site-with-skin")
    fun registerSiteWithSkin(
        @Valid @RequestBody
        request: OnboardingSiteWithSkinRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<OnboardingResponse> {
        return try {
            val result = onboardingService.registerSiteWithSkin(request, token)
            ResponseEntity.ok(
                OnboardingResponse(
                    success = true,
                    message = "사이트 및 스킨이 성공적으로 등록되었습니다.",
                    data = result
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                OnboardingResponse(
                    success = false,
                    message = e.message ?: "사이트 및 스킨 등록에 실패했습니다.",
                    data = null
                )
            )
        } catch (e: SecurityException) {
            ResponseEntity.status(401).body(
                OnboardingResponse(
                    success = false,
                    message = "인증이 필요합니다.",
                    data = null
                )
            )
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(
                OnboardingResponse(
                    success = false,
                    message = "서버 오류가 발생했습니다.",
                    data = null
                )
            )
        }
    }
}
