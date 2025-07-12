package com.comdeply.comment.config

import com.comdeply.comment.entity.User
import com.comdeply.comment.entity.UserRole
import com.comdeply.comment.entity.UserType
import com.comdeply.comment.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OAuth2UserService(
    private val userRepository: UserRepository
) : DefaultOAuth2UserService() {

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId

        val (user, isNewUser) = processOAuth2User(registrationId, oAuth2User)

        return UserPrincipal.create(user, oAuth2User.attributes, isNewUser)
    }

    private fun processOAuth2User(registrationId: String, oAuth2User: OAuth2User): Pair<User, Boolean> {
        val userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.attributes)

        val existingUser = userRepository.findByProviderAndProviderId(registrationId, userInfo.id)

        return if (existingUser != null) {
            Pair(updateExistingUser(existingUser, userInfo), false)
        } else {
            val newUser = createNewUser(registrationId, userInfo)
            Pair(newUser, true)
        }
    }

    private fun createNewUser(provider: String, userInfo: OAuth2UserInfo): User {
        val user = User(
            email = userInfo.email,
            nickname = userInfo.name,
            profileImageUrl = userInfo.imageUrl,
            userType = UserType.OAUTH,
            provider = provider,
            providerId = userInfo.id,
            role = UserRole.USER
        )

        return userRepository.save(user)
    }

    private fun updateExistingUser(existingUser: User, userInfo: OAuth2UserInfo): User {
        val updatedUser = existingUser.copy(
            nickname = userInfo.name,
            profileImageUrl = userInfo.imageUrl,
            updatedAt = LocalDateTime.now()
        )

        return userRepository.save(updatedUser)
    }
}
