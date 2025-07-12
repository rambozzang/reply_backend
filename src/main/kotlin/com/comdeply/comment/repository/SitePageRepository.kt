package com.comdeply.comment.repository

import com.comdeply.comment.entity.PageType
import com.comdeply.comment.entity.SitePage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SitePageRepository : JpaRepository<SitePage, Long> {
    fun findBySiteIdAndIsActiveTrue(siteId: String): List<SitePage>
    fun findBySiteIdAndPageIdAndIsActiveTrue(siteId: String, pageId: String): SitePage?
    fun findBySiteIdAndPageTypeAndIsActiveTrue(siteId: String, pageType: PageType): List<SitePage>
    fun existsBySiteIdAndPageIdAndIsActiveTrue(siteId: String, pageId: String): Boolean
    
    @Query("SELECT DISTINCT sp.siteId FROM SitePage sp WHERE sp.isActive = true")
    fun findAllActiveSiteIds(): List<String>
    
    @Query("SELECT COUNT(sp) FROM SitePage sp WHERE sp.siteId = :siteId AND sp.isActive = true")
    fun countBySiteIdAndIsActiveTrue(siteId: String): Long
}