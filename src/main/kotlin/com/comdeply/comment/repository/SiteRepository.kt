package com.comdeply.comment.repository

import com.comdeply.comment.entity.Site
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SiteRepository : JpaRepository<Site, Long> {

    fun findBySiteKey(siteKey: String): Site?
    fun findByOwnerId(ownerId: Long): List<Site>

    // 사용자별 활성 사이트 수 조회
    fun countByOwnerIdAndIsActiveTrue(ownerId: Long): Long

    // 사이트 이름 중복 확인
    fun findBySiteNameAndOwnerId(siteName: String, ownerId: Long): Site?

    // 사이트 도메인 중복 확인
    fun existsByDomain(domain: String): Boolean

    // 관리자용 추가 쿼리
    fun countBySsoEnabledTrue(): Long
    fun findByDomain(domain: String): Site?
    fun findByIsActiveTrue(): List<Site>

    // existsBySiteKey
    fun existsBySiteKey(siteKey: String): Boolean

    // 여러 사이트 ID의 SSO 활성 사이트 수 (일반 관리자용)
    fun countByIdInAndSsoEnabledTrue(siteIds: List<Long>): Long
}
