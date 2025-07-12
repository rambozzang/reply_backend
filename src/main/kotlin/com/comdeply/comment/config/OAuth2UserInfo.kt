package com.comdeply.comment.config

abstract class OAuth2UserInfo(
    protected val attributes: Map<String, Any>
) {
    abstract val id: String
    abstract val name: String
    abstract val email: String
    abstract val imageUrl: String?
}

class GoogleOAuth2UserInfo(
    attributes: Map<String, Any>
) : OAuth2UserInfo(attributes) {
    override val id: String = attributes["sub"] as String
    override val name: String = attributes["name"] as String
    override val email: String = attributes["email"] as String
    override val imageUrl: String? = attributes["picture"] as String?
}

class KakaoOAuth2UserInfo(
    attributes: Map<String, Any>
) : OAuth2UserInfo(attributes) {
    override val id: String = attributes["id"].toString()
    override val name: String = (attributes["kakao_account"] as Map<*, *>).let { account ->
        (account["profile"] as Map<*, *>)["nickname"] as String
    }
    override val email: String = (attributes["kakao_account"] as Map<*, *>)["email"] as String
    override val imageUrl: String? = (attributes["kakao_account"] as Map<*, *>).let { account ->
        (account["profile"] as Map<*, *>)["profile_image_url"] as String?
    }
}

class NaverOAuth2UserInfo(
    attributes: Map<String, Any>
) : OAuth2UserInfo(attributes) {
    private val response = attributes["response"] as Map<*, *>
    override val id: String = response["id"] as String
    override val name: String = response["nickname"] as String
    override val email: String = response["email"] as String
    override val imageUrl: String? = response["profile_image"] as String?
}

object OAuth2UserInfoFactory {
    fun getOAuth2UserInfo(registrationId: String, attributes: Map<String, Any>): OAuth2UserInfo {
        return when (registrationId) {
            "google" -> GoogleOAuth2UserInfo(attributes)
            "kakao" -> KakaoOAuth2UserInfo(attributes)
            "naver" -> NaverOAuth2UserInfo(attributes)
            else -> throw IllegalArgumentException("Sorry! Login with $registrationId is not supported yet.")
        }
    }
}
