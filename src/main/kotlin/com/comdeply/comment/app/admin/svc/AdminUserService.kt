package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.app.web.svc.UserService
import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.AdminRole
import com.comdeply.comment.repository.AdminRepository
import com.comdeply.comment.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AdminUserService(
    private val userRepository: UserRepository,
    private val adminRepository: AdminRepository,
    private val userService: UserService,
    private val adminPermissionService: AdminPermissionService
) {
    private val logger = LoggerFactory.getLogger(AdminUserService::class.java)

    /**
     * 사용자 목록 조회
     */
    @Transactional(readOnly = true)
    fun getUsers(
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

        try {
            val userListResponse = userService.getUsersByPermission(
                admin = admin,
                page = page,
                size = size,
                search = search,
                siteId = siteId
            )

            logger.info("사용자 목록 조회 완료: adminId={}, 총 {}개", admin.id, userListResponse.totalCount)
            return userListResponse
        } catch (e: Exception) {
            logger.error("사용자 목록 조회 중 오류 발생: adminId={}", admin.id, e)
            throw e
        }
    }

    /**
     * 사용자 상세 조회
     */
    @Transactional(readOnly = true)
    fun getUserDetail(userId: Long, admin: Admin): UserDetailResponse {
        logger.info("사용자 상세 조회: userId={}, adminId={}", userId, admin.id)

        try {
            val userDetail = userService.getUserDetail(userId, admin)

            logger.info("사용자 상세 조회 완료: userId={}, adminId={}", userId, admin.id)
            return userDetail
        } catch (e: Exception) {
            logger.error("사용자 상세 조회 중 오류 발생: userId={}, adminId={}", userId, admin.id, e)
            throw e
        }
    }

    /**
     * 관리자 목록 조회 (SUPER_ADMIN 전용)
     */
    @Transactional(readOnly = true)
    fun getAdmins(admin: Admin, page: Int, size: Int): AdminListResponse {
        logger.info("관리자 목록 조회: adminId={}, page={}, size={}", admin.id, page, size)

        // SUPER_ADMIN만 접근 가능
        if (admin.role != AdminRole.SUPER_ADMIN) {
            throw IllegalArgumentException("해당 기능은 SUPER_ADMIN만 사용할 수 있습니다")
        }

        try {
            val pageable = PageRequest.of(page, size)
            val adminPage = adminRepository.findAll(pageable)

            val adminResponses = adminPage.content.map { adminEntity ->
                convertToAdminResponse(adminEntity)
            }

            val adminListResponse = AdminListResponse(
                admins = adminResponses,
                totalCount = adminPage.totalElements
            )

            logger.info("관리자 목록 조회 완료: adminId={}, 총 {}개", admin.id, adminListResponse.totalCount)
            return adminListResponse
        } catch (e: Exception) {
            logger.error("관리자 목록 조회 중 오류 발생: adminId={}", admin.id, e)
            throw e
        }
    }

    /**
     * Admin 엔티티를 AdminResponse DTO로 변환
     */
    private fun convertToAdminResponse(admin: Admin): AdminResponse {
        return AdminResponse(
            id = admin.id,
            username = admin.username,
            email = admin.email,
            name = admin.name,
            role = admin.role,
            isActive = admin.isActive,
            createdAt = admin.createdAt,
            updatedAt = admin.updatedAt,
            lastLoginAt = admin.lastLoginAt,
            profileImageUrl = admin.profileImageUrl
        )
    }
}
