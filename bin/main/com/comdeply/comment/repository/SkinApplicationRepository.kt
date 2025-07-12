package com.comdeply.comment.repository

import com.comdeply.comment.entity.ApplicationScope
import com.comdeply.comment.entity.SkinApplication
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SkinApplicationRepository : JpaRepository<SkinApplication, Long> {
    fun findBySiteIdAndPageId(siteId: String, pageId: String?): Optional<SkinApplication>
    fun findBySiteIdAndScope(siteId: String, scope: ApplicationScope): List<SkinApplication>
    fun findBySiteId(siteId: String): List<SkinApplication>
    fun findBySkinName(skinName: String): List<SkinApplication>
}
