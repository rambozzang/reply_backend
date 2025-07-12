package com.comdeply.comment.config

import com.comdeply.comment.entity.Admin
import com.comdeply.comment.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User

class UserPrincipal(
    val id: Long,
    val email: String,
    val displayName: String,
    private val password: String?,
    private val authorities: Collection<GrantedAuthority>,
    private val oauthAttributes: MutableMap<String, Any> = mutableMapOf(),
    val isNewUser: Boolean = false
) : OAuth2User,
    UserDetails {
    companion object {
        fun create(user: User): UserPrincipal {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            return UserPrincipal(
                id = user.id,
                email = user.email ?: "",
                displayName = user.nickname,
                password = null,
                authorities = authorities
            )
        }

        fun create(
            user: User,
            attributes: MutableMap<String, Any>,
            isNewUser: Boolean = false
        ): UserPrincipal {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            return UserPrincipal(
                id = user.id,
                email = user.email ?: "",
                displayName = user.nickname,
                password = null,
                authorities = authorities,
                oauthAttributes = attributes,
                isNewUser = isNewUser
            )
        }

        fun createFromAdmin(admin: Admin): UserPrincipal {
            val authorities = listOf(SimpleGrantedAuthority("ROLE_${admin.role.name}"))
            return UserPrincipal(
                id = admin.id,
                email = admin.email,
                displayName = admin.name,
                password = null,
                authorities = authorities
            )
        }
    }

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String? = password

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true

    override fun getAttributes(): MutableMap<String, Any> = oauthAttributes

    override fun getName(): String = id.toString()
}
