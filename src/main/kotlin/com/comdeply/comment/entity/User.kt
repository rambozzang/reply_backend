package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = true)
    val email: String? = null,

    @Column(nullable = false)
    val nickname: String,

    @Column(nullable = true)
    val profileImageUrl: String? = null,

    @Column(nullable = true)
    val password: String? = null, // 기본 회원가입용 비밀번호 (암호화됨)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val userType: UserType = UserType.OAUTH, // 사용자 유형

    @Column(nullable = false)
    val provider: String = "unknown", // google, kakao, naver (OAuth2용)

    @Column(nullable = false)
    val providerId: String = "unknown", // OAuth2 Provider ID

    @Column(nullable = true)
    val guestToken: String? = null, // 익명 사용자 식별용 토큰

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val enabled: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.USER
)

enum class UserRole {
    USER, ADMIN
}

enum class UserType {
    OAUTH, // OAuth2 로그인 사용자 (구글, 카카오, 네이버)
    REGISTERED, // 이메일/비밀번호로 가입한 사용자
    GUEST, // 익명 사용자 (자동 가입)
    ANONYMOUS // 완전 익명 사용자
}
