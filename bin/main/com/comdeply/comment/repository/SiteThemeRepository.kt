package com.comdeply.comment.repository

import com.comdeply.comment.entity.SiteTheme
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SiteThemeRepository : JpaRepository<SiteTheme, Long> {

    // 사이트의 현재 활성 테마 조회 (모든 페이지)
    fun findBySiteIdAndIsActiveTrue(siteId: Long): List<SiteTheme>

    // 특정 사이트의 특정 페이지 테마 조회
    fun findBySiteIdAndPageIdAndIsActiveTrue(siteId: Long, pageId: String): SiteTheme?

    // 사이트의 모든 테마 기록 조회
    fun findBySiteIdOrderByAppliedAtDesc(siteId: Long): List<SiteTheme>

    // 특정 페이지의 테마 기록 조회
    fun findBySiteIdAndPageIdOrderByAppliedAtDesc(siteId: Long, pageId: String): List<SiteTheme>

    fun findBySiteIdAndIsActive(
        siteId: Long,
        isActive: Boolean
    ): List<SiteTheme>

    // 특정 테마를 사용하는 사이트 조회
    fun findBySiteIdAndPageId(
        siteId: Long,
        pageId: String
    ): SiteTheme?

    // 특정 테마를 사용하는 사이트 수 조회
    fun countByThemeIdAndIsActiveTrue(themeId: Long): Long

    // 사이트별 테마 변경 이력
    @Query(
        """
        SELECT st FROM SiteTheme st 
        JOIN FETCH st.theme t 
        WHERE st.siteId = :siteId 
        ORDER BY st.appliedAt DESC
    """
    )
    fun findSiteThemeHistory(@Param("siteId") siteId: Long): List<SiteTheme>

    // 특정 페이지의 테마 변경 이력
    @Query(
        """
        SELECT st FROM SiteTheme st 
        JOIN FETCH st.theme t 
        WHERE st.siteId = :siteId AND st.pageId = :pageId 
        ORDER BY st.appliedAt DESC
    """
    )
    fun findPageThemeHistory(@Param("siteId") siteId: Long, @Param("pageId") pageId: String): List<SiteTheme>

    // 테마별 사용 사이트 목록
    @Query(
        """
        SELECT st FROM SiteTheme st 
        JOIN FETCH st.site s 
        WHERE st.themeId = :themeId AND st.isActive = true
    """
    )
    fun findSitesByTheme(@Param("themeId") themeId: Long): List<SiteTheme>

    // 사이트 테마 존재 여부 확인
    fun existsBySiteId(siteId: Long): Boolean

    // 특정 페이지의 테마 존재 여부 확인
    fun existsBySiteIdAndPageId(siteId: Long, pageId: String): Boolean

    // 사이트별 활성 테마 수 조회 (플랜 검증용)
    fun countBySiteIdAndIsActiveTrue(siteId: Long): Long

    // 특정 사이트의 특정 페이지 테마를 비활성화
    @Modifying
    @Query("UPDATE SiteTheme st SET st.isActive = false WHERE st.siteId = :siteId AND st.pageId = :pageId AND st.isActive = true")
    fun deactivateThemesByPage(@Param("siteId") siteId: Long, @Param("pageId") pageId: String): Int

    // 특정 사이트의 모든 테마를 비활성화
    @Modifying
    @Query("UPDATE SiteTheme st SET st.isActive = false WHERE st.siteId = :siteId AND st.isActive = true")
    fun deactivateAllThemesBySite(@Param("siteId") siteId: Long): Int
}
