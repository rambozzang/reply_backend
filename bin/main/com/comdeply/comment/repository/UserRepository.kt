package com.comdeply.comment.repository

import com.comdeply.comment.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByProviderAndProviderId(provider: String, providerId: String): User?
    fun findByGuestToken(guestToken: String): User?
    fun existsByEmail(email: String): Boolean

    // 검색 기능
    fun findByNicknameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        nickname: String,
        email: String,
        pageable: Pageable
    ): Page<User>

    // 특정 사이트에서 활동한 사용자 조회
    @Query(
        """
        SELECT DISTINCT u FROM User u 
        JOIN Comment c ON u.id = c.userId 
        WHERE c.siteId = :siteId 
        AND (:search IS NULL OR :search = '' OR 
             LOWER(u.nickname) LIKE LOWER(CONCAT('%', :search, '%')) OR 
             LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY u.createdAt DESC
    """
    )
    fun findUsersBySiteId(
        @Param("siteId") siteId: Long,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<User>

    // 여러 사이트에서 활동한 사용자 조회 (사이트 관리자용)
    @Query(
        """
        SELECT DISTINCT u FROM User u 
        JOIN Comment c ON u.id = c.userId 
        WHERE c.siteId IN :siteIds 
        AND (:search IS NULL OR :search = '' OR 
             LOWER(u.nickname) LIKE LOWER(CONCAT('%', :search, '%')) OR 
             LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY u.createdAt DESC
    """
    )
    fun findUsersBySiteIds(
        @Param("siteIds") siteIds: List<Long>,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<User>

    // 전체 사용자 검색 (SUPER_ADMIN용)
    @Query(
        """
        SELECT u FROM User u 
        WHERE (:search IS NULL OR :search = '' OR 
               LOWER(u.nickname) LIKE LOWER(CONCAT('%', :search, '%')) OR 
               LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY u.createdAt DESC
    """
    )
    fun findAllUsersWithSearch(
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<User>

    // 권한별 통계용 쿼리 추가 - 실제로는 SsoUser 테이블에서 조회해야 함
    // fun countBySsoUserTrue(): Long  // 제거 - SsoUserRepository에서 처리

    @Query(
        """
        SELECT COUNT(DISTINCT u) FROM User u 
        JOIN Comment c ON u.id = c.userId 
        WHERE c.siteId IN :siteIds
    """
    )
    fun countBySiteIdIn(@Param("siteIds") siteIds: List<Long>): Long

    // 결제 관리용 추가 메서드
    @Query(
        """
        SELECT DISTINCT u FROM User u 
        JOIN Comment c ON u.id = c.userId 
        WHERE c.siteId IN :siteIds
    """
    )
    fun findBySiteIdIn(@Param("siteIds") siteIds: List<Long>): List<User>
}
