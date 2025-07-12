package com.comdeply.comment.repository

import com.comdeply.comment.entity.Theme
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ThemeRepository : JpaRepository<Theme, Long> {

    // 이름으로 테마 조회
    fun findByName(name: String): Theme?

    // 활성 테마만 조회
    fun findByIsActiveTrue(): List<Theme>

    // 카테고리별 테마 조회
    fun findByCategoryAndIsActiveTrue(category: String): List<Theme>

    // 기본 테마 조회
    fun findByIsBuiltInTrueAndIsActiveTrue(): List<Theme>

    // 프리미엄 테마 조회
    fun findByIsPremiumTrueAndIsActiveTrue(): List<Theme>

    // 사용자가 생성한 테마 조회
    fun findByCreatedByAndIsActiveTrue(createdBy: Long): List<Theme>

    // 인기 테마 조회 (사용량 기준)
    @Query("SELECT t FROM Theme t WHERE t.isActive = true ORDER BY t.usageCount DESC")
    fun findPopularThemes(pageable: Pageable): Page<Theme>

    // 검색 기능
    @Query(
        """
        SELECT t FROM Theme t 
        WHERE t.isActive = true 
        AND (
            LOWER(t.displayName) LIKE LOWER(CONCAT('%', :keyword, '%')) 
            OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(t.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    """
    )
    fun searchThemes(@Param("keyword") keyword: String, pageable: Pageable): Page<Theme>

    // 카테고리별 페이징 조회
    fun findByCategoryAndIsActiveTrueOrderByUsageCountDesc(category: String, pageable: Pageable): Page<Theme>

    // 최근 생성된 테마 조회
    @Query("SELECT t FROM Theme t WHERE t.isActive = true ORDER BY t.createdAt DESC")
    fun findLatestThemes(pageable: Pageable): Page<Theme>

    // 테마 사용량 증가
    @Modifying
    @Query("UPDATE Theme t SET t.usageCount = t.usageCount + 1 WHERE t.id = :themeId")
    fun incrementUsageCount(@Param("themeId") themeId: Long): Int

    // 중복 이름 확인
    fun existsByNameAndIdNot(name: String, id: Long): Boolean

    // 사용자별 테마 개수 조회
    fun countByCreatedByAndIsActiveTrue(createdBy: Long): Long
}
