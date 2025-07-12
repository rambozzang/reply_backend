package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.CommentService
import com.comdeply.comment.app.web.svc.UserService
import com.comdeply.comment.config.JwtTokenProvider
import com.comdeply.comment.dto.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth/v2")
class AuthV2Controller(
    private val userService: UserService,
    private val commentService: CommentService,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @GetMapping("/check-email")
    fun checkEmailExists(@RequestParam email: String): ResponseEntity<Map<String, Boolean>> {
        val exists = userService.existsByEmail(email)
        return ResponseEntity.ok(mapOf("exists" to exists))
    }

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody
        request: RegisterRequest
    ): ResponseEntity<AuthResponse> {
        try {
            val user = userService.registerUser(request)
            val token = jwtTokenProvider.generateTokenFromUserId(user.id)

            val response = AuthResponse(
                token = token,
                user = user,
                expiresIn = jwtTokenProvider.getTokenExpirationTime()
            )

            return ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody
        request: LoginRequest
    ): ResponseEntity<AuthResponse> {
        try {
            val user = userService.loginUser(request)
            val token = jwtTokenProvider.generateTokenFromUserId(user.id)

            val response = AuthResponse(
                token = token,
                user = user,
                expiresIn = jwtTokenProvider.getTokenExpirationTime()
            )

            return ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.status(401).build()
        }
    }

    @PostMapping("/guest/comment")
    fun createGuestComment(
        @Valid @RequestBody
        request: GuestCommentRequest,
        @RequestHeader("Guest-Token", required = false) guestToken: String?
    ): ResponseEntity<Map<String, Any>> {
        try {
            // 익명 사용자 생성 또는 조회
            val guestUser = userService.getOrCreateGuestUser(request)

            // 댓글 생성
            val comment = commentService.createGuestComment(request, guestUser)

            // JWT 토큰 생성
            val token = jwtTokenProvider.generateTokenFromUserId(guestUser.id)

            val userResponse = UserResponse(
                id = guestUser.id,
                email = guestUser.email,
                nickname = guestUser.nickname,
                profileImageUrl = guestUser.profileImageUrl,
                userType = guestUser.userType,
                provider = guestUser.provider,
                createdAt = guestUser.createdAt,
                updatedAt = guestUser.updatedAt,
                isActive = guestUser.isActive,
                role = guestUser.role
            )

            val response: Map<String, Any> = mapOf(
                "comment" to comment,
                "user" to userResponse,
                "token" to token,
                "guestToken" to (guestUser.guestToken ?: "")
            )

            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }
    }
}
