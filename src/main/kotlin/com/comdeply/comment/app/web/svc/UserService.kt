package com.comdeply.comment.app.web.svc

import com.comdeply.comment.app.admin.svc.AdminPermissionService
import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.User
import com.comdeply.comment.entity.UserRole
import com.comdeply.comment.entity.UserType
import com.comdeply.comment.repository.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val siteRepository: SiteRepository,
    private val adminPermissionService: AdminPermissionService,
    private val passwordEncoder: PasswordEncoder
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    /**
     * 권한에 따른 사용자 목록 조회
     */
    fun getUsersByPermission(
        admin: Admin,
        page: Int,
        size: Int,
        search: String?,
        siteId: Long?
    ): UserListResponse {
        logger.info(
            "사용자 목록 조회: adminId={}, page={}, size={}, search={}, siteId={}",
            admin.id,
            page,
            size,
            search,
            siteId
        )

        val pageable = PageRequest.of(page, size)
        val accessibleSiteIds = adminPermissionService.getAccessibleSiteIds(admin)

        val usersPage = when {
            // 특정 사이트 필터가 있는 경우
            siteId != null -> {
                // 해당 사이트에 대한 권한 확인
                if (!adminPermissionService.hasPermissionForSite(admin, siteId) && !adminPermissionService.canViewGlobalStats(admin)) {
                    throw IllegalArgumentException("해당 사이트에 대한 권한이 없습니다")
                }
                userRepository.findUsersBySiteId(siteId, search, pageable)
            }
            // SUPER_ADMIN은 모든 사용자 조회
            adminPermissionService.canViewGlobalStats(admin) -> {
                userRepository.findAllUsersWithSearch(search, pageable)
            }
            // 일반 ADMIN은 할당된 사이트의 사용자만 조회
            accessibleSiteIds.isNotEmpty() -> {
                userRepository.findUsersBySiteIds(accessibleSiteIds, search, pageable)
            }
            // 접근 권한이 없는 경우
            else -> {
                throw IllegalArgumentException("사용자 조회 권한이 없습니다")
            }
        }

        val usersWithSite = usersPage.content.map { user ->
            getUserWithSiteInfo(user.id, accessibleSiteIds, admin)
        }

        return UserListResponse(
            users = usersWithSite,
            totalCount = usersPage.totalElements,
            currentPage = page,
            totalPages = usersPage.totalPages
        )
    }

    /**
     * 사용자 상세 정보 조회
     */
    fun getUserDetail(userId: Long, admin: Admin): UserDetailResponse {
        logger.info("사용자 상세 조회: userId={}, adminId={}", userId, admin.id)

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다: $userId") }

        val accessibleSiteIds = adminPermissionService.getAccessibleSiteIds(admin)

        // 사용자의 사이트별 활동 정보 조회
        val siteActivities = if (adminPermissionService.canViewGlobalStats(admin)) {
            // SUPER_ADMIN은 모든 사이트 활동 조회
            getSiteActivitiesForUser(userId, null)
        } else if (accessibleSiteIds.isNotEmpty()) {
            // 일반 ADMIN은 할당된 사이트 활동만 조회
            getSiteActivitiesForUser(userId, accessibleSiteIds)
        } else {
            emptyList()
        }

        // 최근 댓글 조회 (권한에 따라)
        val recentComments = if (adminPermissionService.canViewGlobalStats(admin)) {
            getRecentCommentsForUser(userId, null, 10)
        } else if (accessibleSiteIds.isNotEmpty()) {
            getRecentCommentsForUser(userId, accessibleSiteIds, 10)
        } else {
            emptyList()
        }

        val totalCommentCount = siteActivities.sumOf { it.commentCount }

        return UserDetailResponse(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl,
            userType = user.userType,
            provider = user.provider,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            isActive = user.isActive,
            role = user.role,
            totalCommentCount = totalCommentCount,
            activeSites = siteActivities,
            recentComments = recentComments
        )
    }

    /**
     * 사용자별 사이트 정보 조회
     */
    private fun getUserWithSiteInfo(userId: Long, accessibleSiteIds: List<Long>, admin: Admin): UserWithSiteResponse {
        val user = userRepository.findById(userId).orElse(null) ?: return createEmptyUserResponse(userId)

        val siteInfos = if (adminPermissionService.canViewGlobalStats(admin)) {
            getSiteInfosForUser(userId, null)
        } else if (accessibleSiteIds.isNotEmpty()) {
            getSiteInfosForUser(userId, accessibleSiteIds)
        } else {
            emptyList()
        }

        val totalCommentCount = siteInfos.sumOf { it.commentCount }
        val primarySite = siteInfos.maxByOrNull { it.commentCount }

        return UserWithSiteResponse(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl,
            userType = user.userType,
            provider = user.provider,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            isActive = user.isActive,
            role = user.role,
            commentCount = totalCommentCount,
            primarySite = primarySite,
            activeSites = siteInfos
        )
    }

    /**
     * 사용자의 사이트 정보 목록 조회
     */
    private fun getSiteInfosForUser(userId: Long, siteIds: List<Long>?): List<SiteInfo> {
        // 간단한 구현 - 실제로는 사용자별 사이트 댓글 수를 집계하는 쿼리 필요
        val comments = if (siteIds.isNullOrEmpty()) {
            commentRepository.findByUserIdAndIsDeletedFalse(userId)
        } else {
            commentRepository.findByUserIdAndSiteIdInAndIsDeletedFalse(userId, siteIds)
        }

        val siteCommentCounts = comments.groupBy { it.siteId }
            .mapValues { (_, comments) -> comments.size.toLong() }

        return siteCommentCounts.map { (siteId, count) ->
            val site = siteRepository.findById(siteId).orElse(null)
            SiteInfo(
                siteId = siteId,
                siteName = site?.siteName,
                siteDomain = site?.domain ?: "Unknown",
                commentCount = count
            )
        }.sortedByDescending { it.commentCount }
    }

    /**
     * 사용자의 사이트별 활동 정보 조회
     */
    private fun getSiteActivitiesForUser(userId: Long, siteIds: List<Long>?): List<SiteActivityInfo> {
        val siteInfos = getSiteInfosForUser(userId, siteIds)
        return siteInfos.map { siteInfo ->
            val lastComment = commentRepository.findTopByUserIdAndSiteIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, siteInfo.siteId)
            SiteActivityInfo(
                siteId = siteInfo.siteId,
                siteName = siteInfo.siteName,
                siteDomain = siteInfo.siteDomain,
                commentCount = siteInfo.commentCount,
                lastCommentAt = lastComment?.createdAt
            )
        }
    }

    /**
     * 사용자의 최근 댓글 조회
     */
    private fun getRecentCommentsForUser(userId: Long, siteIds: List<Long>?, limit: Int): List<RecentCommentInfo> {
        val comments = if (siteIds.isNullOrEmpty()) {
            commentRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
        } else {
            commentRepository.findByUserIdAndSiteIdInAndIsDeletedFalseOrderByCreatedAtDesc(userId, siteIds, PageRequest.of(0, limit))
        }

        return comments.content.map { comment ->
            val site = siteRepository.findById(comment.siteId).orElse(null)
            RecentCommentInfo(
                id = comment.id,
                content = comment.content,
                siteId = comment.siteId,
                siteName = site?.siteName,
                siteDomain = site?.domain ?: "Unknown",
                pageId = comment.pageId,
                createdAt = comment.createdAt
            )
        }
    }

    private fun createEmptyUserResponse(userId: Long): UserWithSiteResponse {
        return UserWithSiteResponse(
            id = userId,
            email = null,
            nickname = "Unknown User",
            profileImageUrl = null,
            userType = UserType.GUEST,
            provider = "unknown",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            isActive = false,
            role = UserRole.USER,
            commentCount = 0,
            primarySite = null,
            activeSites = emptyList()
        )
    }

    /**
     * ID로 사용자 조회
     */
    fun getUserById(id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }

    /**
     * 이메일로 사용자 존재 여부 확인
     */
    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    /**
     * 사용자 등록
     */
    @Transactional
    fun registerUser(request: RegisterRequest): UserResponse {
        if (existsByEmail(request.email)) {
            throw IllegalArgumentException("이미 존재하는 이메일입니다")
        }

        val user = User(
            email = request.email,
            nickname = request.nickname,
            profileImageUrl = null,
            userType = UserType.REGISTERED,
            provider = "local",
            providerId = request.email,
            isActive = true,
            enabled = true,
            role = UserRole.USER
        )

        val savedUser = userRepository.save(user)
        return UserResponse(
            id = savedUser.id,
            email = savedUser.email,
            nickname = savedUser.nickname,
            profileImageUrl = savedUser.profileImageUrl,
            userType = savedUser.userType,
            provider = savedUser.provider,
            createdAt = savedUser.createdAt,
            updatedAt = savedUser.updatedAt,
            isActive = savedUser.isActive,
            role = savedUser.role
        )
    }

    /**
     * 사용자 로그인
     */
    fun loginUser(request: LoginRequest): UserResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("존재하지 않는 사용자입니다")

        if (!user.isActive) {
            throw IllegalArgumentException("비활성화된 사용자입니다")
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다")
        }

        return UserResponse(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl,
            userType = user.userType,
            provider = user.provider,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            isActive = user.isActive,
            role = user.role
        )
    }

    /**
     * 게스트 사용자 생성 또는 조회
     */
    @Transactional
    fun getOrCreateGuestUser(request: GuestCommentRequest): User {
        // 이메일이 있는 경우 기존 사용자 확인
        val existingUser = if (!request.email.isNullOrBlank()) {
            userRepository.findByEmail(request.email)
        } else {
            null
        }

        return existingUser ?: run {
            val guestUser = User(
                email = request.email,
                nickname = request.nickname,
                profileImageUrl = null,
                userType = UserType.GUEST,
                provider = "guest",
                providerId = request.email ?: "guest_${System.currentTimeMillis()}",
                isActive = true,
                enabled = true,
                role = UserRole.USER
            )
            userRepository.save(guestUser)
        }
    }

    /**
     * 익명 사용자 생성
     */
    @Transactional
    fun createAnonymousUser(): User {
        val anonymousUser = User(
            email = null,
            nickname = "익명",
            profileImageUrl = null,
            userType = UserType.ANONYMOUS,
            provider = "anonymous",
            providerId = "anonymous_${System.currentTimeMillis()}",
            isActive = true,
            enabled = true,
            role = UserRole.USER
        )
        return userRepository.save(anonymousUser)
    }
}
