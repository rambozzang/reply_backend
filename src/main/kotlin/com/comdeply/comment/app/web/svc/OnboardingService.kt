package com.comdeply.comment.app.web.svc

import com.comdeply.comment.config.JwtTokenProvider
import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.*
import com.comdeply.comment.portone.service.PortOneBillingService
import com.comdeply.comment.portone.service.PortOneSubscriptionService
import com.comdeply.comment.repository.*
import com.comdeply.comment.utils.PlanLimits
import com.comdeply.comment.utils.PlanType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class OnboardingService(
    private val adminRepository: AdminRepository,
    private val userRepository: UserRepository,
    private val siteRepository: SiteRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val paymentRepository: PaymentRepository,
    private val themeRepository: ThemeRepository,
    private val siteThemeRepository: SiteThemeRepository,
    private val adminSiteRepository: AdminSiteRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val portOneBillingService: PortOneBillingService,
    private val portOneSubscriptionService: PortOneSubscriptionService
) {

    /**
     * 1단계: 관리자 계정 등록
     */
    fun registerAdmin(request: OnboardingAdminRegisterRequest): OnboardingAdminResponse {
        // 이메일 중복 체크
        if (adminRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("이미 사용 중인 이메일입니다.")
        }

        // 사용자명 중복 체크
        if (adminRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("이미 사용 중인 사용자명입니다.")
        }

        // 관리자 계정 생성
        val admin = Admin(
            username = request.username,
            password = passwordEncoder.encode(request.password),
            email = request.email,
            name = request.name,
            role = AdminRole.ADMIN, // 온보딩으로 생성되는 계정은 기본 ADMIN 역할
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedAdmin = adminRepository.save(admin)

        // JWT 토큰 생성
        val token = jwtTokenProvider.generateTokenFromAdminId(savedAdmin.id)

        return OnboardingAdminResponse(
            adminId = savedAdmin.id,
            username = savedAdmin.username,
            email = savedAdmin.email,
            name = savedAdmin.name,
            token = token,
            expiresIn = jwtTokenProvider.getTokenExpirationTime()
        )
    }

    /**
     * 1단계: 관리자 계정 로그인
     */
    fun loginAdmin(request: OnboardingAdminLoginRequest): OnboardingAdminResponse {
        val admin = adminRepository.findByUsername(request.username)
            ?: throw IllegalArgumentException("존재하지 않는 ID 입니다.")

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password, admin.password)) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }

        // JWT 토큰 생성
        val token = jwtTokenProvider.generateTokenFromAdminId(admin.id)

        return OnboardingAdminResponse(
            adminId = admin.id,
            username = admin.username,
            email = admin.email,
            name = admin.name,
            token = token,
            expiresIn = jwtTokenProvider.getTokenExpirationTime()
        )
    }

    /**
     * 2단계: 사이트 등록
     */
    fun registerSite(request: OnboardingSiteRegisterRequest, token: String): OnboardingSiteResponse {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { SecurityException("유효하지 않은 관리자입니다.") }

        // 도메인 중복 체크
        if (siteRepository.existsByDomain(request.domain)) {
            throw IllegalArgumentException("이미 등록된 도메인입니다.")
        }

        // 사이트 키 생성 (UUID 기반)
        val siteKey = generateSiteKey()

        // 사이트 생성
        val site = Site(
            ownerId = adminId,
            siteName = request.siteName,
            domain = request.domain,
            siteKey = siteKey,
            isActive = true,
            themeColor = request.themeColor ?: "#007bff",
            requireAuth = request.requireAuth ?: false,
            enableModeration = request.enableModeration ?: true,
            theme = "light",
            language = "ko",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedSite = siteRepository.save(site)

        // 관리자-사이트 연결 생성
        val adminSite = AdminSite(
            adminId = adminId,
            siteId = savedSite.id,
            permission = SitePermission.MANAGE,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            assignedBy = adminId
        )
        adminSiteRepository.save(adminSite)

        return OnboardingSiteResponse(
            siteId = savedSite.id,
            siteKey = savedSite.siteKey,
            siteName = savedSite.siteName,
            domain = savedSite.domain,
            themeColor = savedSite.themeColor ?: "#007bff",
            requireAuth = savedSite.requireAuth,
            enableModeration = savedSite.enableModeration
        )
    }

    /**
     * 2단계: 스킨 적용
     */
    fun applySkin(request: OnboardingSkinApplyRequest, token: String): OnboardingSkinResponse {
        val adminId = extractAdminIdFromToken(token)

        // 사이트 조회 및 권한 체크
        val site = siteRepository.findBySiteKey(request.siteKey)
            ?: throw IllegalArgumentException("존재하지 않는 사이트입니다.")

        if (site.ownerId != adminId) {
            throw SecurityException("해당 사이트에 대한 권한이 없습니다.")
        }

        // 테마 조회
        val theme = themeRepository.findByName(request.skinName)
            ?: throw IllegalArgumentException("존재하지 않는 테마입니다.")

        // 기존 사이트 테마 비활성화
        siteThemeRepository.deactivateThemesByPage(site.id, request.pageId ?: "default")

        // 새 사이트 테마 적용
        val siteTheme = SiteTheme(
            siteId = site.id,
            themeId = theme.id,
            pageId = request.pageId ?: "default",
            customizations = request.customizations ?: "{}",
            isActive = true,
            appliedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedSiteTheme = siteThemeRepository.save(siteTheme)

        return OnboardingSkinResponse(
            siteThemeId = savedSiteTheme.id,
            siteKey = site.siteKey,
            themeName = theme.name,
            themeDisplayName = theme.displayName,
            pageId = savedSiteTheme.pageId,
            appliedAt = savedSiteTheme.appliedAt
        )
    }

    /**
     * 3단계: 무료 구독 생성
     */
    fun createFreeSubscription(request: OnboardingFreeSubscriptionRequest, token: String): OnboardingSubscriptionResponse {
        val adminId = extractAdminIdFromToken(token)

        // 관리자 조회
        val admin = adminRepository.findById(adminId)
            .orElseThrow { SecurityException("유효하지 않은 관리자입니다.") }

        // 기존 활성 구독 체크
        val existingSubscription = subscriptionRepository.findByAdminIdAndStatus(adminId, SubscriptionStatus.ACTIVE)
        if (existingSubscription != null) {
            throw IllegalArgumentException("이미 활성화된 구독이 있습니다.")
        }

        // STARTER 플랜 정보
        val planLimits = PlanLimits.getByPlan(PlanType.STARTER)

        // 무료 구독 생성
        val subscription = Subscription(
            admin = admin,
            planId = "starter",
            planName = "Starter",
            amount = 0,
            status = SubscriptionStatus.ACTIVE,
            startDate = LocalDateTime.now(),
            endDate = LocalDateTime.now().plusYears(100), // 무료 플랜은 만료 없음
            nextBillingDate = null, // 무료 플랜은 청구 없음
            autoRenewal = false,
            monthlyCommentLimit = planLimits.monthlyCommentLimit,
            currentCommentCount = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedSubscription = subscriptionRepository.save(subscription)

        return OnboardingSubscriptionResponse(
            subscriptionId = savedSubscription.id,
            planId = savedSubscription.planId,
            planName = savedSubscription.planName,
            amount = savedSubscription.amount,
            status = savedSubscription.status.name,
            startDate = savedSubscription.startDate,
            endDate = savedSubscription.endDate,
            monthlyCommentLimit = savedSubscription.monthlyCommentLimit
        )
    }

    /**
     * 3단계: 유료 구독 생성 (PortOne 결제 포함)
     */
    fun createPaidSubscription(request: OnboardingPaidSubscriptionRequest, token: String): OnboardingPaidSubscriptionResponse {
        val adminId = extractAdminIdFromToken(token)

        // 관리자 조회
        val admin = adminRepository.findById(adminId)
            .orElseThrow { SecurityException("유효하지 않은 관리자입니다.") }

        // 기존 활성 구독 체크
        val existingSubscription = subscriptionRepository.findByAdminIdAndStatus(adminId, SubscriptionStatus.ACTIVE)
        if (existingSubscription != null) {
            throw IllegalArgumentException("이미 활성화된 구독이 있습니다.")
        }

        // 플랜 정보 조회
        val planType = PlanType.valueOf(request.planId.uppercase())
        val planLimits = PlanLimits.getByPlan(planType)
        val planAmount = when (planType) {
            PlanType.PRO -> 29000
            PlanType.ENTERPRISE -> 99000
            else -> throw IllegalArgumentException("유료 플랜만 선택 가능합니다.")
        }

        return try {
            // 1. PortOne 빌링키 발급
            val billingKey = portOneBillingService.issueBillingKey(
                adminId = adminId,
                cardNumber = request.cardNumber,
                expiry = request.expiry,
                birth = request.birth,
                pwd2digit = request.pwd2digit
            )

            // 2. 첫 번째 구독 결제 실행
            val subscription = portOneSubscriptionService.startSubscription(
                adminId = adminId,
                plan = com.comdeply.comment.app.web.entity.SubscriptionPlan.valueOf(planType.name)
            )

            OnboardingPaidSubscriptionResponse(
                subscriptionId = subscription.id!!,
                planId = subscription.plan.name.lowercase(),
                planName = subscription.plan.displayName,
                amount = planLimits.monthlyCommentLimit, // TODO: 실제 가격 정보 추가 필요
                status = subscription.status.name,
                startDate = subscription.startDate,
                endDate = subscription.endDate,
                nextBillingDate = subscription.nextBillingDate,
                monthlyCommentLimit = planLimits.monthlyCommentLimit,
                paymentId = 0L, // TODO: Get actual payment ID from subscription service
                paymentStatus = "PAID" // TODO: Get actual payment status
            )
        } catch (e: Exception) {
            // 결제 실패 시 롤백 처리
            throw RuntimeException("결제 처리 중 오류가 발생했습니다: ${e.message}", e)
        }
    }

    /**
     * 4단계: 완료 정보 조회 (임베드 코드 생성)
     */
    fun getCompletionInfo(siteKey: String, token: String): OnboardingCompletionResponse {
        val adminId = extractAdminIdFromToken(token)

        // 사이트 조회 및 권한 체크
        val site = siteRepository.findBySiteKey(siteKey)
            ?: throw IllegalArgumentException("존재하지 않는 사이트입니다.")

        if (site.ownerId != adminId) {
            throw SecurityException("해당 사이트에 대한 권한이 없습니다.")
        }

        // 적용된 테마 정보 조회
        val siteThemes = siteThemeRepository.findBySiteIdAndIsActive(site.id, true)
        val themeInfo = siteThemes.firstOrNull()?.let { siteTheme ->
            themeRepository.findById(siteTheme.themeId).orElse(null)
        }

        // 관리자의 구독 정보 조회
        val subscription = subscriptionRepository.findByAdminIdAndStatus(adminId, SubscriptionStatus.ACTIVE)

        // 임베드 코드 생성
        val embedCode = generateEmbedCode(siteKey, themeInfo?.name)

        return OnboardingCompletionResponse(
            siteKey = site.siteKey,
            siteName = site.siteName,
            domain = site.domain,
            themeName = themeInfo?.displayName ?: "기본 테마",
            planName = subscription?.planName ?: "플랜 없음",
            embedCode = embedCode,
            dashboardUrl = "/dashboard/sites/${site.siteKey}",
            documentationUrl = "/docs/integration"
        )
    }

    /**
     * 이메일 중복 체크
     */
    fun checkEmailExists(email: String): Boolean {
        return adminRepository.existsByEmail(email) || userRepository.existsByEmail(email)
    }

    /**
     * 도메인 중복 체크
     */
    fun checkDomainExists(domain: String): Boolean {
        return siteRepository.existsByDomain(domain)
    }

    /**
     * 사용자명 중복 체크
     */
    fun checkUsernameExists(username: String): Boolean {
        return adminRepository.existsByUsername(username)
    }

    /**
     * 사용 가능한 테마 목록 조회
     */
    fun getAvailableThemes(planId: String): List<OnboardingThemeResponse> {
        val planType = try {
            PlanType.valueOf(planId.uppercase())
        } catch (e: Exception) {
            PlanType.STARTER
        }

        val planLimits = PlanLimits.getByPlan(planType)
        val isPremiumAllowed = planType != PlanType.FREE && planType != PlanType.STARTER

        return themeRepository.findByIsActiveTrue()
            .filter { theme ->
                if (theme.isPremium) isPremiumAllowed else true
            }
            .take(planLimits.themeLimit)
            .map { theme ->
                OnboardingThemeResponse(
                    id = theme.id,
                    name = theme.name,
                    displayName = theme.displayName,
                    description = theme.description ?: "",
                    category = theme.category ?: "general",
                    thumbnailUrl = theme.thumbnailUrl,
                    isPremium = theme.isPremium,
                    isBuiltIn = theme.isBuiltIn
                )
            }
    }

    // === Private Helper Methods ===

    private fun extractAdminIdFromToken(token: String): Long {
        val cleanToken = token.replace("Bearer ", "")
        return jwtTokenProvider.getAdminIdFromToken(cleanToken)
            ?: throw SecurityException("유효하지 않은 토큰입니다.")
    }

    // 더 이상 사용하지 않음 - Admin이 직접 구독과 결제를 관리
    // private fun getOrCreateUserFromAdmin(adminId: Long): User { ... }

    private fun generateSiteKey(): String {
        var siteKey: String
        do {
            siteKey = UUID.randomUUID().toString().replace("-", "").substring(0, 16)
        } while (siteRepository.existsBySiteKey(siteKey))
        return siteKey
    }

    /**
     * 2단계: 사이트 및 스킨 통합 등록
     */
    fun registerSiteWithSkin(request: OnboardingSiteWithSkinRequest, token: String): OnboardingSiteWithSkinResponse {
        val adminId = extractAdminIdFromToken(token)
        val admin = adminRepository.findById(adminId)
            .orElseThrow { SecurityException("유효하지 않은 관리자입니다.") }

        // 도메인 중복 체크
        if (siteRepository.existsByDomain(request.domain)) {
            throw IllegalArgumentException("이미 등록된 도메인입니다.")
        }

        // 테마 조회
        val theme = themeRepository.findByName(request.skinName)
            ?: throw IllegalArgumentException("존재하지 않는 테마입니다.")

        // 사이트 키 생성 (UUID 기반)
        val siteKey = generateSiteKey()

        // 사이트 생성
        val site = Site(
            ownerId = adminId,
            siteName = request.siteName,
            domain = request.domain,
            siteKey = siteKey,
            isActive = true,
            themeColor = request.themeColor ?: "#007bff",
            requireAuth = request.requireAuth ?: false,
            enableModeration = request.enableModeration ?: true,
            theme = "light",
            language = "ko",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedSite = siteRepository.save(site)

        // 관리자-사이트 연결 생성
        val adminSite = AdminSite(
            adminId = adminId,
            siteId = savedSite.id,
            permission = SitePermission.MANAGE,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            assignedBy = adminId
        )
        adminSiteRepository.save(adminSite)

        // 사이트 테마 적용
        val siteTheme = SiteTheme(
            siteId = savedSite.id,
            themeId = theme.id,
            pageId = request.pageId ?: "default",
            customizations = request.customizations ?: "{}",
            isActive = true,
            appliedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedSiteTheme = siteThemeRepository.save(siteTheme)

        // 응답 생성
        val siteResponse = OnboardingSiteResponse(
            siteId = savedSite.id,
            siteKey = savedSite.siteKey,
            siteName = savedSite.siteName,
            domain = savedSite.domain,
            themeColor = savedSite.themeColor ?: "#007bff",
            requireAuth = savedSite.requireAuth,
            enableModeration = savedSite.enableModeration
        )

        val skinResponse = OnboardingSkinResponse(
            siteThemeId = savedSiteTheme.id,
            siteKey = savedSite.siteKey,
            themeName = theme.name,
            themeDisplayName = theme.displayName,
            pageId = savedSiteTheme.pageId,
            appliedAt = savedSiteTheme.appliedAt
        )

        return OnboardingSiteWithSkinResponse(
            site = siteResponse,
            skin = skinResponse
        )
    }

    private fun generateEmbedCode(siteKey: String, themeName: String?): String {
        val themeParam = themeName?.let { "&theme=$it" } ?: ""
        return """
            <!-- Comdeply 댓글 위젯 -->
            <div id="comdeply-comments"></div>
            <script>
              (function() {
                var script = document.createElement('script');
                script.src = 'https://cdn.comdeply.com/embed.js?siteKey=$siteKey$themeParam';
                script.async = true;
                document.head.appendChild(script);
              })();
            </script>
        """.trimIndent()
    }
}
