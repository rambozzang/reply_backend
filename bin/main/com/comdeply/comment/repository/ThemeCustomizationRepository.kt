package com.comdeply.comment.repository

import com.comdeply.comment.entity.ThemeCustomization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ThemeCustomizationRepository : JpaRepository<ThemeCustomization, Long> {

    // 사이트별 커스터마이징 조회
    fun findBySiteIdAndIsActiveTrueOrderByCreatedAtDesc(siteId: Long): List<ThemeCustomization>

    // 사용자별 커스터마이징 조회
    fun findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId: Long): List<ThemeCustomization>

    // 사이트 + 테마별 커스터마이징 조회
    fun findBySiteIdAndThemeIdAndIsActiveTrueOrderByCreatedAtDesc(siteId: Long, themeId: Long): List<ThemeCustomization>

    // 사용자의 특정 테마 커스터마이징 조회
    fun findByUserIdAndThemeIdAndIsActiveTrueOrderByCreatedAtDesc(userId: Long, themeId: Long): List<ThemeCustomization>

    // 사이트의 활성 커스터마이징 조회
    @Query(
        """
        SELECT tc FROM ThemeCustomization tc 
        WHERE tc.siteId = :siteId 
        AND tc.isActive = true 
        ORDER BY tc.updatedAt DESC 
        LIMIT 1
    """
    )
    fun findActiveBySiteId(@Param("siteId") siteId: Long): ThemeCustomization?

    // 커스터마이징 이름 중복 확인 (같은 사이트 내)
    fun existsBySiteIdAndNameAndIdNot(siteId: Long, name: String, id: Long): Boolean

    // 사용자별 커스터마이징 개수 조회
    fun countByUserIdAndIsActiveTrue(userId: Long): Long

    // 사이트별 커스터마이징 개수 조회
    fun countBySiteIdAndIsActiveTrue(siteId: Long): Long
}
