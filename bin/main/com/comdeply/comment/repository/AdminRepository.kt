package com.comdeply.comment.repository

import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.AdminRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AdminRepository : JpaRepository<Admin, Long> {
    // 사용자명으로 관리자 조회
    fun findByUsername(username: String): Admin?

    // 이메일로 관리자 조회
    fun findByEmail(email: String): Admin?

    // 사용자명 또는 이메일로 관리자 조회
    @Query("SELECT a FROM Admin a WHERE a.username = :usernameOrEmail OR a.email = :usernameOrEmail")
    fun findByUsernameOrEmail(
        @Param("usernameOrEmail") usernameOrEmail: String
    ): Admin?

    // 활성 관리자만 조회
    fun findByIsActiveTrue(): List<Admin>

    // 역할별 관리자 조회
    fun findByRole(role: AdminRole): List<Admin>

    // 활성 관리자 중 역할별 조회
    fun findByRoleAndIsActiveTrue(role: AdminRole): List<Admin>

    // 사용자명 존재 여부 확인
    fun existsByUsername(username: String): Boolean

    // 이메일 존재 여부 확인
    fun existsByEmail(email: String): Boolean

    // 마지막 로그인 시간 이후 관리자 조회
    fun findByLastLoginAtAfter(lastLoginAt: LocalDateTime): List<Admin>

    // 생성일 기준 관리자 조회
    fun findByCreatedAtBetween(
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Admin>

    // 활성 관리자 수 조회
    fun countByIsActiveTrue(): Long

    // 역할별 관리자 수 조회
    fun countByRole(role: AdminRole): Long
}
