package com.comdeply.comment.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2AuthenticationFailureHandler : SimpleUrlAuthenticationFailureHandler() {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        val targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/auth/callback")
            .queryParam("error", exception.message ?: "Authentication failed")
            .build().toUriString()

        logger.debug("OAuth2 authentication failed: ${exception.message}")
        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}
