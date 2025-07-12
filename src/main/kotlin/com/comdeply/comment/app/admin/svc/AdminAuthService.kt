package com.comdeply.comment.app.admin.svc

import com.comdeply.comment.app.admin.svc.vo.EmailCheckResponse
import com.comdeply.comment.app.admin.svc.vo.LogoutResponse
import com.comdeply.comment.app.admin.svc.vo.UsernameCheckResponse
import com.comdeply.comment.dto.*
import com.comdeply.comment.entity.Admin
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AdminAuthService(
    private val adminService: AdminService
) {
    private val logger = LoggerFactory.getLogger(AdminAuthService::class.java)

    /**
     * 관리자 로그인 처리
     */
    fun login(request: AdminLoginRequest): AdminAuthResponse {
        logger.info("관리자 로그인 처리 시작: {}", request.usernameOrEmail)

        try {
            val response = adminService.login(request)
            logger.info("관리자 로그인 성공: {}", request.usernameOrEmail)
            return response
        } catch (e: IllegalArgumentException) {
            logger.warn("관리자 로그인 실패: usernameOrEmail={}, error={}", request.usernameOrEmail, e.message)
            throw e
        } catch (e: Exception) {
            logger.error("관리자 로그인 중 예상치 못한 오류 발생: usernameOrEmail={}", request.usernameOrEmail, e)
            throw e
        }
    }

    /**
     * 관리자 회원가입 처리
     */
    fun register(request: AdminRegisterRequest): AdminRegisterResponse {
        logger.info("관리자 회원가입 처리 시작: username={}, email={}", request.username, request.email)

        try {
            // 중복 체크
            if (adminService.existsByUsername(request.username)) {
                throw IllegalArgumentException("이미 존재하는 사용자명입니다")
            }

            if (adminService.existsByEmail(request.email)) {
                throw IllegalArgumentException("이미 존재하는 이메일입니다")
            }

            val adminResponse = adminService.register(request)
            val registerResponse = AdminRegisterResponse(
                admin = adminResponse,
                isNewUser = true
            )

            logger.info("관리자 회원가입 성공: username={}, email={}", request.username, request.email)
            return registerResponse
        } catch (e: IllegalArgumentException) {
            logger.warn(
                "관리자 회원가입 실패: username={}, email={}, error={}",
                request.username,
                request.email,
                e.message
            )
            throw e
        } catch (e: Exception) {
            logger.error(
                "관리자 회원가입 중 예상치 못한 오류 발생: username={}, email={}",
                request.username,
                request.email,
                e
            )
            throw e
        }
    }

    /**
     * 현재 로그인한 관리자 정보 조회
     */
    @Transactional(readOnly = true)
    fun getCurrentAdmin(adminId: Long): AdminResponse {
        logger.info("현재 관리자 정보 조회: adminId={}", adminId)

        val admin = adminService.findById(adminId)
            ?: throw IllegalArgumentException("유효하지 않은 관리자입니다")

        if (!admin.isActive) {
            throw IllegalArgumentException("비활성화된 관리자입니다")
        }

        val adminResponse = AdminResponse(
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

        logger.info("현재 관리자 정보 조회 성공: adminId={}, username={}", adminId, admin.username)
        return adminResponse
    }

    /**
     * 사용자명 중복 확인
     */
    @Transactional(readOnly = true)
    fun checkUsername(username: String): UsernameCheckResponse {
        logger.info("사용자명 중복 확인: username={}", username)

        if (username.isBlank()) {
            throw IllegalArgumentException("사용자명을 입력해주세요")
        }

        if (username.length < 3) {
            throw IllegalArgumentException("사용자명은 3자 이상이어야 합니다")
        }

        if (username.length > 20) {
            throw IllegalArgumentException("사용자명은 20자 이하여야 합니다")
        }

        // 영문, 숫자, 언더스코어만 허용
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            throw IllegalArgumentException("사용자명은 영문, 숫자, 언더스코어만 사용할 수 있습니다")
        }

        val exists = adminService.existsByUsername(username)
        val response = UsernameCheckResponse(
            username = username,
            exists = exists,
            message = if (exists) "이미 사용 중인 사용자명입니다" else "사용 가능한 사용자명입니다"
        )

        logger.info("사용자명 중복 확인 완료: username={}, exists={}", username, exists)
        return response
    }

    /**
     * 이메일 중복 확인
     */
    @Transactional(readOnly = true)
    fun checkEmail(email: String): EmailCheckResponse {
        logger.info("이메일 중복 확인: email={}", email)

        if (email.isBlank()) {
            throw IllegalArgumentException("이메일을 입력해주세요")
        }

        // 간단한 이메일 형식 검증
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!email.matches(emailPattern)) {
            throw IllegalArgumentException("올바른 이메일 형식이 아닙니다")
        }

        val exists = adminService.existsByEmail(email)
        val response = EmailCheckResponse(
            email = email,
            exists = exists,
            message = if (exists) "이미 사용 중인 이메일입니다" else "사용 가능한 이메일입니다"
        )

        logger.info("이메일 중복 확인 완료: email={}, exists={}", email, exists)
        return response
    }

    /**
     * 토큰으로 관리자 정보 조회
     */
    @Transactional(readOnly = true)
    fun getAdminByToken(token: String): Admin? {
        logger.info("토큰으로 관리자 정보 조회")

        return try {
            val admin = adminService.getAdminByToken(token)
            if (admin != null) {
                logger.info("토큰으로 관리자 정보 조회 성공: adminId={}", admin.id)
            } else {
                logger.warn("토큰으로 관리자 정보 조회 실패: 유효하지 않은 토큰")
            }
            admin
        } catch (e: Exception) {
            logger.error("토큰으로 관리자 정보 조회 중 오류 발생", e)
            null
        }
    }

    /**
     * 관리자 프로필 업데이트
     */
    fun updateProfile(adminId: Long, request: AdminUpdateRequest): AdminResponse {
        logger.info("관리자 프로필 업데이트: adminId={}", adminId)

        val admin = adminService.findById(adminId)
            ?: throw IllegalArgumentException("유효하지 않은 관리자입니다")

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

        logger.info("관리자 프로필 업데이트 성공: adminId={}, username={}", adminId, admin.username)
        return updatedAdmin
    }

    /**
     * 관리자 비밀번호 변경
     */
    fun changePassword(adminId: Long, request: AdminPasswordChangeRequest): AdminResponse {
        logger.info("관리자 비밀번호 변경: adminId={}", adminId)

        val admin = adminService.findById(adminId)
            ?: throw IllegalArgumentException("유효하지 않은 관리자입니다")

        if (!admin.isActive) {
            throw IllegalArgumentException("비활성화된 관리자입니다")
        }

        val updatedAdmin = adminService.changePassword(adminId, request)

        logger.info("관리자 비밀번호 변경 성공: adminId={}, username={}", adminId, admin.username)
        return updatedAdmin
    }

    /**
     * 로그아웃 처리 (클라이언트에서 토큰 제거용)
     */
    fun logout(adminId: Long): LogoutResponse {
        logger.info("관리자 로그아웃 처리: adminId={}", adminId)

        // 실제로는 JWT 토큰 기반이므로 서버에서 특별한 처리는 불필요
        // 클라이언트에서 토큰을 제거하면 됨
        // 향후 토큰 블랙리스트 등의 기능을 추가할 수 있음

        return LogoutResponse(
            message = "성공적으로 로그아웃되었습니다",
            timestamp = System.currentTimeMillis()
        )
    }
}
