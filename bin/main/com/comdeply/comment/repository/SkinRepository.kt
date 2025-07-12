package com.comdeply.comment.repository

import com.comdeply.comment.entity.Skin
import com.comdeply.comment.entity.SkinType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SkinRepository : JpaRepository<Skin, Long> {
    fun findByName(name: String): Optional<Skin>
    fun findByFolderName(folderName: String): Optional<Skin>
    fun findByType(type: SkinType): List<Skin>
    fun existsByName(name: String): Boolean
    fun existsByFolderName(folderName: String): Boolean

    // 관리자별 접근 가능한 스킨 조회 (공통 + 본인 스킨)
    fun findByTypeInOrAdminId(types: List<SkinType>, adminId: Long): List<Skin>

    // 관리자의 개인 스킨만 조회
    fun findByAdminId(adminId: Long): List<Skin>

    // 공유 가능한 스킨 조회 (BUILT_IN + SHARED)
    fun findByTypeIn(types: List<SkinType>): List<Skin>
    
    // 공유된 스킨 또는 특정 관리자의 스킨 조회 (관리자용)
    fun findByIsSharedTrueOrAdminId(adminId: Long): List<Skin>
}
