package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "themes")
data class Theme(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false, length = 50)
    val name: String,

    @Column(nullable = false, length = 100)
    val displayName: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(nullable = false, length = 50)
    val category: String = "general", // general, modern, minimal, classic, dark, etc.

    @Column(nullable = false)
    val isBuiltIn: Boolean = false, // 시스템 기본 제공 테마 여부

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    val isPremium: Boolean = false, // 프리미엄 테마 여부

    // 테마 색상 설정 (JSON 형태)
    @Column(columnDefinition = "TEXT", nullable = false)
    val colors: String = "{}",

    // 타이포그래피 설정 (JSON 형태)
    @Column(columnDefinition = "TEXT", nullable = false)
    val typography: String = "{}",

    // 간격 설정 (JSON 형태)
    @Column(columnDefinition = "TEXT", nullable = false)
    val spacing: String = "{}",

    // 테두리 반경 설정 (JSON 형태)
    @Column(columnDefinition = "TEXT", nullable = false)
    val borderRadius: String = "{}",

    // 컴포넌트별 설정 (JSON 형태)
    @Column(columnDefinition = "TEXT", nullable = false)
    val components: String = "{}",

    // 사용자 정의 CSS
    @Column(columnDefinition = "TEXT")
    val customCss: String? = null,

    // 썸네일 이미지 URL
    @Column(length = 500)
    val thumbnailUrl: String? = null,

    // 미리보기 이미지 URL
    @Column(length = 500)
    val previewUrl: String? = null,

    // 생성자 정보
    @Column(nullable = true)
    val createdBy: Long? = null, // 사용자가 만든 커스텀 테마인 경우

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    // 사용 통계
    @Column(nullable = false)
    val usageCount: Long = 0, // 이 테마를 사용하는 사이트 수

    // 버전 관리
    @Column(nullable = false, length = 20)
    val version: String = "1.0.0",

    // 태그 (검색용)
    @Column(columnDefinition = "TEXT")
    val tags: String? = null // comma-separated tags
)
