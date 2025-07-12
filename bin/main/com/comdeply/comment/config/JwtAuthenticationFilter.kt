package com.comdeply.comment.config

import com.comdeply.comment.app.admin.svc.AdminService
import com.comdeply.comment.app.web.svc.UserService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val tokenProvider: JwtTokenProvider,
    private val userService: UserService,
    private val adminService: AdminService,
    @Value("\${jwt.secret}")
    private val jwtSecret: String
) : OncePerRequestFilter() {

    private val key by lazy { Keys.hmacShaKeyFor(jwtSecret.toByteArray()) }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 상담 신청 API는 JWT 검증 제외
        if (request.requestURI.startsWith("/api/contact/")) {
            filterChain.doFilter(request, response)
            return
        }

        val jwt = getJwtFromRequest(request)

        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt!!)) {
            try {
                // 토큰에서 클레임 추출
                val claims = Jwts
                    .parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .body

                val userId = claims.subject.toLong()
                val tokenType = claims["type"] as String?

                // 관리자 토큰인 경우
                if (tokenType == "admin") {
                    val admin = adminService.findById(userId)
                    if (admin != null) {
                        val userPrincipal = UserPrincipal.createFromAdmin(admin)
                        val authentication = UsernamePasswordAuthenticationToken(
                            userPrincipal,
                            null,
                            userPrincipal.authorities
                        )
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authentication
                    }
                } else {
                    // 일반 사용자 토큰인 경우
                    val user = userService.getUserById(userId)
                    if (user != null) {
                        val userPrincipal = UserPrincipal.create(user)
                        val authentication = UsernamePasswordAuthenticationToken(
                            userPrincipal,
                            null,
                            userPrincipal.authorities
                        )
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authentication
                    }
                }
            } catch (e: Exception) {
                logger.error("JWT 처리 중 오류 발생", e)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
