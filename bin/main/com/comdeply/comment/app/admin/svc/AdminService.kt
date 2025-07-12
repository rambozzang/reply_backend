package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.config.JwtTokenProvider
import com.comdeply.comment.dto.AdminAuthResponse
import com.comdeply.comment.dto.AdminCreateRequest
import com.comdeply.comment.dto.AdminLoginRequest
import com.comdeply.comment.dto.AdminPasswordChangeRequest
import com.comdeply.comment.dto.AdminRegisterRequest
import com.comdeply.comment.dto.AdminResponse
import com.comdeply.comment.dto.AdminStatsResponse
import com.comdeply.comment.dto.AdminUpdateRequest
import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.AdminRole
import com.comdeply.comment.repository.AdminRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class AdminService(
    private val adminRepository: AdminRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {
    private val logger = LoggerFactory.getLogger(AdminService::class.java)

    /**
     * ID로 관리자 조회
     */
    fun findById(id: Long): Admin? {
        return adminRepository.findById(id).orElse(null)
    }

    /**
     * 관리자 로그인 처리 및 JWT 토큰 발급
     */
    fun login(request: AdminLoginRequest): AdminAuthResponse {
        logger.info("관리자 로그인 시도: {}", request.usernameOrEmail)

        val admin = adminRepository.findByUsernameOrEmail(request.usernameOrEmail)
            ?: throw IllegalArgumentException("존재하지 않는 관리자입니다")

        if (!admin.isActive) {
            throw IllegalArgumentException("비활성화된 관리자입니다")
        }

        if (!passwordEncoder.matches(request.password, admin.password)) {
            throw IllegalArgumentException("잘못된 비밀번호입니다")
        }

        // 마지막 로그인 시간 업데이트
        val updatedAdmin = admin.copy(
            lastLoginAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        adminRepository.save(updatedAdmin)

        // JWT 토큰 생성 (관리자 전용)
        val token = jwtTokenProvider.generateAdminToken(admin.id)

        logger.info("관리자 로그인 성공: {}", admin.username)

        return AdminAuthResponse(
            token = token,
            admin = convertToResponse(updatedAdmin),
            expiresIn = 86400 // 24시간 (초 단위)
        )
    }

    /**
     * 관리자 회원가입 처리
     */
    fun register(request: AdminRegisterRequest): AdminResponse {
        logger.info("관리자 회원가입 시도: {}", request.username)

        // 중복 검사
        if (adminRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("이미 존재하는 사용자ID입니다")
        }

        if (adminRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("이미 존재하는 이메일입니다")
        }

        // 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(request.password)

        val admin = Admin(
            username = request.username,
            password = encodedPassword,
            email = request.email,
            name = request.name,
            role = request.role,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedAdmin = adminRepository.save(admin)

        logger.info("관리자 회원가입 성공: {}", savedAdmin.username)

        return convertToResponse(savedAdmin)
    }

    /**
     * 관리자 정보 조회
     */
    @Transactional(readOnly = true)
    fun getAdmin(adminId: Long): AdminResponse {
        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 관리자입니다") }

        return convertToResponse(admin)
    }

    /**
     * 페이징된 관리자 목록 조회
     */
    @Transactional(readOnly = true)
    fun getAdmins(pageable: Pageable): Page<AdminResponse> {
        return adminRepository.findAll(pageable).map { convertToResponse(it) }
    }

    /**
     * 활성 상태인 관리자 목록 조회
     */
    @Transactional(readOnly = true)
    fun getActiveAdmins(): List<AdminResponse> {
        return adminRepository.findByIsActiveTrue().map { convertToResponse(it) }
    }

    /**
     * 특정 역할의 활성 관리자 목록 조회
     */
    @Transactional(readOnly = true)
    fun getAdminsByRole(role: AdminRole): List<AdminResponse> {
        return adminRepository.findByRoleAndIsActiveTrue(role).map { convertToResponse(it) }
    }

    /**
     * 관리자 정보 수정
     */
    fun updateAdmin(adminId: Long, request: AdminUpdateRequest): AdminResponse {
        logger.info("관리자 정보 수정: {}", adminId)

        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 관리자입니다") }

        // 이메일 중복 검사 (변경하는 경우)
        if (request.email != null && request.email != admin.email) {
            if (adminRepository.existsByEmail(request.email)) {
                throw IllegalArgumentException("이미 존재하는 이메일입니다")
            }
        }

        val updatedAdmin = admin.copy(
            name = request.name ?: admin.name,
            email = request.email ?: admin.email,
            profileImageUrl = request.profileImageUrl ?: admin.profileImageUrl,
            isActive = request.isActive ?: admin.isActive,
            updatedAt = LocalDateTime.now()
        )

        val savedAdmin = adminRepository.save(updatedAdmin)

        logger.info("관리자 정보 수정 완료: {}", savedAdmin.username)

        return convertToResponse(savedAdmin)
    }

    /**
     * 관리자 비밀번호 변경
     */
    fun changePassword(adminId: Long, request: AdminPasswordChangeRequest): AdminResponse {
        logger.info("관리자 비밀번호 변경: {}", adminId)

        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 관리자입니다") }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword, admin.password)) {
            throw IllegalArgumentException("현재 비밀번호가 올바르지 않습니다")
        }

        // 새 비밀번호 암호화
        val encodedNewPassword = passwordEncoder.encode(request.newPassword)

        val updatedAdmin = admin.copy(
            password = encodedNewPassword,
            updatedAt = LocalDateTime.now()
        )

        val savedAdmin = adminRepository.save(updatedAdmin)

        logger.info("관리자 비밀번호 변경 완료: {}", savedAdmin.username)

        return convertToResponse(savedAdmin)
    }

    /**
     * 관리자 삭제 (비활성화 처리)
     */
    fun deleteAdmin(adminId: Long): AdminResponse {
        logger.info("관리자 삭제: {}", adminId)

        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 관리자입니다") }

        val updatedAdmin = admin.copy(
            isActive = false,
            updatedAt = LocalDateTime.now()
        )

        val savedAdmin = adminRepository.save(updatedAdmin)

        logger.info("관리자 삭제 완료: {}", savedAdmin.username)

        return convertToResponse(savedAdmin)
    }

    /**
     * 관리자 역할 변경
     */
    fun changeAdminRole(adminId: Long, newRole: AdminRole): AdminResponse {
        logger.info("관리자 역할 변경: adminId={}, newRole={}", adminId, newRole)

        val admin = adminRepository.findById(adminId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 관리자입니다") }

        val updatedAdmin = admin.copy(
            role = newRole,
            updatedAt = LocalDateTime.now()
        )

        val savedAdmin = adminRepository.save(updatedAdmin)

        logger.info("관리자 역할 변경 완료: {} -> {}", admin.role, newRole)

        return convertToResponse(savedAdmin)
    }

    /**
     * 새 관리자 생성 (SUPER_ADMIN 전용)
     */
    fun createAdmin(request: AdminCreateRequest): AdminResponse {
        logger.info("관리자 생성: {}", request.username)

        // 중복 검사
        if (adminRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("이미 존재하는 사용자ID입니다")
        }

        if (adminRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("이미 존재하는 이메일입니다")
        }

        // 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(request.password)

        val admin = Admin(
            username = request.username,
            password = encodedPassword,
            email = request.email,
            name = request.name,
            role = request.role,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedAdmin = adminRepository.save(admin)

        logger.info("관리자 생성 완료: {} (역할: {})", savedAdmin.username, savedAdmin.role)

        return convertToResponse(savedAdmin)
    }

    /**
     * 관리자 통계 정보 조회
     */
    @Transactional(readOnly = true)
    fun getAdminStats(): AdminStatsResponse {
        val now = LocalDateTime.now()
        val weekAgo = now.minusDays(7)

        val totalAdmins = adminRepository.count()
        val activeAdmins = adminRepository.countByIsActiveTrue()
        val superAdmins = adminRepository.countByRole(AdminRole.SUPER_ADMIN)
        val siteAdmins = adminRepository.countByRole(AdminRole.ADMIN)
        val moderators = adminRepository.countByRole(AdminRole.MODERATOR)
        val recentLogins = adminRepository.findByLastLoginAtAfter(weekAgo).size.toLong()

        return AdminStatsResponse(
            totalAdmins = totalAdmins,
            activeAdmins = activeAdmins,
            superAdmins = superAdmins,
            siteAdmins = siteAdmins,
            moderators = moderators,
            recentLogins = recentLogins
        )
    }

    /**
     * 사용자명으로 관리자 존재 여부 확인
     */
    @Transactional(readOnly = true)
    fun existsByUsername(username: String): Boolean {
        return adminRepository.existsByUsername(username)
    }

    /**
     * 이메일로 관리자 존재 여부 확인
     */
    @Transactional(readOnly = true)
    fun existsByEmail(email: String): Boolean {
        return adminRepository.existsByEmail(email)
    }

    /**
     * JWT 토큰으로 관리자 정보 조회 (Admin 엔티티 반환)
     */
    @Transactional(readOnly = true)
    fun getAdminByToken(token: String): Admin? {
        return try {
            val adminId = jwtTokenProvider.getUserIdFromToken(token)
            val admin = adminRepository.findById(adminId.toLong())
                .orElse(null)

            if (admin?.isActive == true) {
                admin
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("토큰으로 관리자 조회 실패", e)
            null
        }
    }

    /**
     * JWT 토큰으로 관리자 응답 조회 (AdminResponse 반환)
     */
    @Transactional(readOnly = true)
    fun getAdminResponseByToken(token: String): AdminResponse? {
        return try {
            val admin = getAdminByToken(token)
            if (admin != null) {
                convertToResponse(admin)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("토큰으로 관리자 응답 조회 실패", e)
            null
        }
    }

    /**
     * 모든 관리자 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    fun getAllAdmins(page: Int, size: Int): Page<AdminResponse> {
        val pageable = PageRequest.of(page, size)
        return adminRepository.findAll(pageable).map { convertToResponse(it) }
    }

    /**
     * Admin 엔티티를 AdminResponse DTO로 변환
     */
    private fun convertToResponse(admin: Admin): AdminResponse {
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
