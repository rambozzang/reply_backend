package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.app.admin.svc.vo.AdminActivationResponse
import com.comdeply.comment.app.admin.svc.vo.AdminActivityLogResponse
import com.comdeply.comment.app.admin.svc.vo.AdminDetailResponse
import com.comdeply.comment.app.admin.svc.vo.AdminListResponse
import com.comdeply.comment.app.admin.svc.vo.AdminStatisticsResponse
import com.comdeply.comment.app.admin.svc.vo.PasswordChangeResponse
import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.AdminRole
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AdminManagementService(
    private val adminService: AdminService,
    private val adminPermissionService: AdminPermissionService
) {
    private val logger = LoggerFactory.getLogger(AdminManagementService::class.java)

    /**
     * 관리자 프로필 조회
     */
    @Transactional(readOnly = true)
    fun getProfile(adminId: Long): AdminResponse {
        logger.info("관리자 프로필 조회: adminId={}", adminId)

        val admin = adminService.findById(adminId)
            ?: throw IllegalArgumentException("관리자 정보를 찾을 수 없습니다")

        if (!admin.isActive) {
            throw IllegalArgumentException("비활성화된 관리자입니다")
        }

        val response = adminService.getAdmin(adminId)
        logger.info("관리자 프로필 조회 완료: adminId={}, username={}", adminId, admin.username)
        return response
    }

    /**
     * 관리자 프로필 업데이트
     */
    fun updateProfile(adminId: Long, request: AdminUpdateRequest): AdminResponse {
        logger.info("관리자 프로필 업데이트: adminId={}", adminId)

        val admin = adminService.findById(adminId)
            ?: throw IllegalArgumentException("관리자 정보를 찾을 수 없습니다")

        if (!admin.isActive) {
            throw IllegalArgumentException("비활성화된 관리자입니다")
        }

        // 이메일 변경 시 중복 확인
        if (request.email != null && request.email != admin.email) {
            if (adminService.existsByEmail(request.email)) {
                throw IllegalArgumentException("이미 사용 중인 이메일입니다")
            }
        }

        val updatedAdmin = adminService.updateAdmin(adminId, request)
        logger.info("관리자 프로필 업데이트 완료: adminId={}, username={}", adminId, admin.username)
        return updatedAdmin
    }

    /**
     * 관리자 비밀번호 변경
     */
    fun changePassword(adminId: Long, request: AdminPasswordChangeRequest): PasswordChangeResponse {
        logger.info("관리자 비밀번호 변경: adminId={}", adminId)

        val admin = adminService.findById(adminId)
            ?: throw IllegalArgumentException("관리자 정보를 찾을 수 없습니다")

        if (!admin.isActive) {
            throw IllegalArgumentException("비활성화된 관리자입니다")
        }

        adminService.changePassword(adminId, request)

        logger.info("관리자 비밀번호 변경 완료: adminId={}, username={}", adminId, admin.username)
        return PasswordChangeResponse(
            message = "비밀번호가 성공적으로 변경되었습니다",
            changedAt = System.currentTimeMillis()
        )
    }

    /**
     * 관리자 목록 조회 (SUPER_ADMIN 전용)
     */
    @Transactional(readOnly = true)
    fun getAdminList(currentAdmin: Admin, page: Int, size: Int): AdminListResponse {
        logger.info("관리자 목록 조회: currentAdminId={}, page={}, size={}", currentAdmin.id, page, size)

        // SUPER_ADMIN만 모든 관리자 목록을 조회할 수 있음
        if (currentAdmin.role != AdminRole.SUPER_ADMIN) {
            throw IllegalArgumentException("관리자 목록 조회 권한이 없습니다")
        }

        val pageable = PageRequest.of(page, size)
        val adminListResponse = adminService.getAdmins(pageable)

        logger.info("관리자 목록 조회 완료: 총 {}개", adminListResponse.totalElements)
        return AdminListResponse(
            admins = adminListResponse.content,
            totalElements = adminListResponse.totalElements,
            totalPages = adminListResponse.totalPages,
            currentPage = adminListResponse.number,
            size = adminListResponse.size

        )
    }

    /**
     * 관리자 상세 정보 조회
     */
    @Transactional(readOnly = true)
    fun getAdminDetail(currentAdmin: Admin, targetAdminId: Long): AdminDetailResponse {
        logger.info("관리자 상세 조회: currentAdminId={}, targetAdminId={}", currentAdmin.id, targetAdminId)

        // SUPER_ADMIN만 다른 관리자 정보를 조회할 수 있음 (자신의 정보는 제외)
        if (currentAdmin.role != AdminRole.SUPER_ADMIN && currentAdmin.id != targetAdminId) {
            throw IllegalArgumentException("해당 관리자 정보에 대한 접근 권한이 없습니다")
        }

        val admin = adminService.findById(targetAdminId)
            ?: throw IllegalArgumentException("관리자 정보를 찾을 수 없습니다")

        val adminResponse = adminService.getAdmin(targetAdminId)

        // 관리자의 사이트 권한 정보 조회
        val sitePermissions = adminPermissionService.getAdminSitePermissions(targetAdminId)

        val response = AdminDetailResponse(
            admin = adminResponse,
            sitePermissions = sitePermissions,
            canManage = adminPermissionService.canManageAdmin(currentAdmin, admin)
        )

        logger.info("관리자 상세 조회 완료: targetAdminId={}, username={}", targetAdminId, admin.username)
        return response
    }

    /**
     * 새 관리자 생성 (SUPER_ADMIN 전용)
     */
    fun createAdmin(currentAdmin: Admin, request: AdminCreateRequest): AdminResponse {
        logger.info("새 관리자 생성: currentAdminId={}, username={}", currentAdmin.id, request.username)

        // SUPER_ADMIN만 새 관리자를 생성할 수 있음
        if (currentAdmin.role != AdminRole.SUPER_ADMIN) {
            throw IllegalArgumentException("관리자 생성 권한이 없습니다")
        }

        // 중복 체크
        if (adminService.existsByUsername(request.username)) {
            throw IllegalArgumentException("이미 존재하는 사용자명입니다")
        }

        if (adminService.existsByEmail(request.email)) {
            throw IllegalArgumentException("이미 존재하는 이메일입니다")
        }

        val newAdmin = adminService.createAdmin(request)

        logger.info(
            "새 관리자 생성 완료: adminId={}, username={}, role={}",
            newAdmin.id,
            newAdmin.username,
            newAdmin.role
        )
        return newAdmin
    }

    /**
     * 관리자 정보 수정 (SUPER_ADMIN 전용)
     */
    fun updateAdmin(currentAdmin: Admin, targetAdminId: Long, request: AdminUpdateRequest): AdminResponse {
        logger.info("관리자 정보 수정: currentAdminId={}, targetAdminId={}", currentAdmin.id, targetAdminId)

        // SUPER_ADMIN만 다른 관리자 정보를 수정할 수 있음 (자신의 정보는 제외)
        if (currentAdmin.role != AdminRole.SUPER_ADMIN && currentAdmin.id != targetAdminId) {
            throw IllegalArgumentException("해당 관리자 정보 수정 권한이 없습니다")
        }

        val targetAdmin = adminService.findById(targetAdminId)
            ?: throw IllegalArgumentException("관리자 정보를 찾을 수 없습니다")

        // 이메일 변경 시 중복 확인
        if (request.email != null && request.email != targetAdmin.email) {
            if (adminService.existsByEmail(request.email)) {
                throw IllegalArgumentException("이미 사용 중인 이메일입니다")
            }
        }

        val updatedAdmin = adminService.updateAdmin(targetAdminId, request)

        logger.info("관리자 정보 수정 완료: targetAdminId={}, username={}", targetAdminId, targetAdmin.username)
        return updatedAdmin
    }

    /**
     * 관리자 역할 변경 (SUPER_ADMIN 전용)
     */
    fun changeAdminRole(currentAdmin: Admin, targetAdminId: Long, newRole: AdminRole): AdminResponse {
        logger.info(
            "관리자 역할 변경: currentAdminId={}, targetAdminId={}, newRole={}",
            currentAdmin.id,
            targetAdminId,
            newRole
        )

        // SUPER_ADMIN만 역할을 변경할 수 있음
        if (currentAdmin.role != AdminRole.SUPER_ADMIN) {
            throw IllegalArgumentException("관리자 역할 변경 권한이 없습니다")
        }

        // 자기 자신의 역할은 변경할 수 없음
        if (currentAdmin.id == targetAdminId) {
            throw IllegalArgumentException("자신의 역할은 변경할 수 없습니다")
        }

        val targetAdmin = adminService.findById(targetAdminId)
            ?: throw IllegalArgumentException("관리자 정보를 찾을 수 없습니다")

        val updatedAdmin = adminService.changeAdminRole(targetAdminId, newRole)

        logger.info(
            "관리자 역할 변경 완료: targetAdminId={}, username={}, oldRole={}, newRole={}",
            targetAdminId,
            targetAdmin.username,
            targetAdmin.role,
            newRole
        )
        return updatedAdmin
    }

    /**
     * 관리자 활성화 (SUPER_ADMIN 전용)
     */
    fun activateAdmin(currentAdmin: Admin, targetAdminId: Long): AdminActivationResponse {
        logger.info("관리자 활성화: currentAdminId={}, targetAdminId={}", currentAdmin.id, targetAdminId)

        // SUPER_ADMIN만 관리자 상태를 변경할 수 있음
        if (currentAdmin.role != AdminRole.SUPER_ADMIN) {
            throw IllegalArgumentException("관리자 상태 변경 권한이 없습니다")
        }

        val targetAdmin = adminService.findById(targetAdminId)
            ?: throw IllegalArgumentException("관리자 정보를 찾을 수 없습니다")

        if (targetAdmin.isActive) {
            throw IllegalArgumentException("이미 활성화된 관리자입니다")
        }

        val updateRequest = AdminUpdateRequest(
            name = null,
            email = null,
            profileImageUrl = null,
            isActive = true
        )
        adminService.updateAdmin(targetAdminId, updateRequest)

        logger.info("관리자 활성화 완료: targetAdminId={}, username={}", targetAdminId, targetAdmin.username)
        return AdminActivationResponse(
            adminId = targetAdminId,
            username = targetAdmin.username,
            isActive = true,
            message = "관리자가 성공적으로 활성화되었습니다",
            changedAt = System.currentTimeMillis()
        )
    }

    /**
     * 관리자 비활성화 (SUPER_ADMIN 전용)
     */
    fun deactivateAdmin(currentAdmin: Admin, targetAdminId: Long): AdminActivationResponse {
        logger.info("관리자 비활성화: currentAdminId={}, targetAdminId={}", currentAdmin.id, targetAdminId)

        // SUPER_ADMIN만 관리자 상태를 변경할 수 있음
        if (currentAdmin.role != AdminRole.SUPER_ADMIN) {
            throw IllegalArgumentException("관리자 상태 변경 권한이 없습니다")
        }

        // 자기 자신은 비활성화할 수 없음
        if (currentAdmin.id == targetAdminId) {
            throw IllegalArgumentException("자신을 비활성화할 수 없습니다")
        }

        val targetAdmin = adminService.findById(targetAdminId)
            ?: throw IllegalArgumentException("관리자 정보를 찾을 수 없습니다")

        if (!targetAdmin.isActive) {
            throw IllegalArgumentException("이미 비활성화된 관리자입니다")
        }

        val updateRequest = AdminUpdateRequest(
            name = null,
            email = null,
            profileImageUrl = null,
            isActive = false
        )
        adminService.updateAdmin(targetAdminId, updateRequest)

        logger.info("관리자 비활성화 완료: targetAdminId={}, username={}", targetAdminId, targetAdmin.username)
        return AdminActivationResponse(
            adminId = targetAdminId,
            username = targetAdmin.username,
            isActive = false,
            message = "관리자가 성공적으로 비활성화되었습니다",
            changedAt = System.currentTimeMillis()
        )
    }

    /**
     * 관리자 통계 조회 (SUPER_ADMIN 전용)
     */
    @Transactional(readOnly = true)
    fun getAdminStatistics(currentAdmin: Admin): AdminStatisticsResponse {
        logger.info("관리자 통계 조회: currentAdminId={}", currentAdmin.id)

        // SUPER_ADMIN만 관리자 통계를 조회할 수 있음
        if (currentAdmin.role != AdminRole.SUPER_ADMIN) {
            throw IllegalArgumentException("관리자 통계 조회 권한이 없습니다")
        }

        val stats = adminService.getAdminStats()

        val response = AdminStatisticsResponse(
            totalAdmins = stats.totalAdmins,
            activeAdmins = stats.activeAdmins,
            superAdmins = stats.superAdmins,
            siteAdmins = stats.siteAdmins,
            moderators = stats.moderators,
            recentLogins = stats.recentLogins,
            inactiveAdmins = stats.totalAdmins - stats.activeAdmins,
            lastUpdated = System.currentTimeMillis()
        )

        logger.info("관리자 통계 조회 완료: 총 {}명, 활성 {}명", response.totalAdmins, response.activeAdmins)
        return response
    }

    /**
     * 관리자 활동 로그 조회
     */
    @Transactional(readOnly = true)
    fun getAdminActivityLogs(currentAdmin: Admin, targetAdminId: Long?, page: Int, size: Int): AdminActivityLogResponse {
        logger.info(
            "관리자 활동 로그 조회: currentAdminId={}, targetAdminId={}, page={}, size={}",
            currentAdmin.id,
            targetAdminId,
            page,
            size
        )

        // SUPER_ADMIN만 활동 로그를 조회할 수 있음
        if (currentAdmin.role != AdminRole.SUPER_ADMIN) {
            throw IllegalArgumentException("관리자 활동 로그 조회 권한이 없습니다")
        }

        // 실제 구현에서는 활동 로그 테이블에서 데이터를 조회해야 함
        // 현재는 기본적인 구조만 제공
        val response = AdminActivityLogResponse(
            logs = emptyList(), // 실제 구현 시 로그 데이터 조회
            totalElements = 0,
            totalPages = 0,
            currentPage = page,
            size = size
        )

        logger.info("관리자 활동 로그 조회 완료: 총 {}개", response.totalElements)
        return response
    }
}
