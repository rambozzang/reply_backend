package com.comdeply.comment.config

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Date

@Component
class JwtTokenProvider {
    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration}")
    private val jwtExpiration: Long = 86400000 // 24시간

    private val key: Key by lazy { Keys.hmacShaKeyFor(jwtSecret.toByteArray()) }

    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserPrincipal
        val expiryDate = Date(Date().time + jwtExpiration)

        return Jwts
            .builder()
            .setSubject(userPrincipal.id.toString())
            .setIssuedAt(Date())
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    // generateTokenFromAdminId
    fun generateTokenFromAdminId(adminId: Long): String {
        val expiryDate = Date(Date().time + jwtExpiration)

        return Jwts
            .builder()
            .setSubject(adminId.toString())
            .claim("type", "admin")
            .setIssuedAt(Date())
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    // getAdminIdFromToken
    fun getAdminIdFromToken(token: String): Long {
        val claims =
            Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

        return claims.subject.toLong()
    }

    fun generateTokenFromUserId(userId: Long): String {
        val expiryDate = Date(Date().time + jwtExpiration)

        return Jwts
            .builder()
            .setSubject(userId.toString())
            .setIssuedAt(Date())
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    fun generateAdminToken(adminId: Long): String {
        val expiryDate = Date(Date().time + jwtExpiration)

        return Jwts
            .builder()
            .setSubject(adminId.toString())
            .claim("type", "admin")
            .setIssuedAt(Date())
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    fun getUserIdFromToken(token: String): Long {
        val claims =
            Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

        return claims.subject.toLong()
    }

    fun validateToken(token: String): Boolean {
        try {
            Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            return true
        } catch (ex: MalformedJwtException) {
            println("Invalid JWT token")
        } catch (ex: ExpiredJwtException) {
            println("Expired JWT token")
        } catch (ex: UnsupportedJwtException) {
            println("Unsupported JWT token")
        } catch (ex: IllegalArgumentException) {
            println("JWT claims string is empty")
        }
        return false
    }

    fun getTokenExpirationTime(): Long {
        return jwtExpiration / 1000 // 초 단위로 반환
    }
}
