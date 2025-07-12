package com.comdeply.comment.app.web.svc

/**
 * FCM 메시지 전송 서비스 사용 예시:
 *
 * // 1. 단일 사용자에게 메시지 전송
 * fcmService.sendMessageToUser("userId", FcmMessageDto(
 *     title = "새로운 메시지",
 *     body = "홍길동님이 메시지를 보냈습니다.",
 *     imageUrl = "https://example.com/profile/user123.jpg",
 *     data = mapOf(
 *         "messageId" to "msg123",
 *         "senderId" to "user123"
 *     )
 * ))
 *
 * // 2. 여러 사용자에게 메시지 전송
 * fcmService.sendMessageToUsers(
 *     userIds = listOf("user1", "user2"),
 *     message = FcmMessageDto(
 *         title = "새로운 공지사항",
 *         body = "2024년 회사 워크샵 안내",
 *         imageUrl = "https://example.com/notice/workshop.jpg",
 *         data = mapOf("noticeId" to "notice123")
 *     )
 * )
 *
 * // 3. FCM 토큰으로 직접 전송
 * fcmService.sendMessageToToken("fcmToken", FcmMessageDto(
 *     title = "알림 제목",
 *     body = "알림 내용",
 *     imageUrl = "https://example.com/images/notification.jpg"
 * ))
 *
 * // 4. 여러 FCM 토큰으로 직접 전송
 * fcmService.sendMessageToTokens(
 *     tokens = listOf("token1", "token2"),
 *     message = FcmMessageDto(
 *         title = "단체 알림 제목",
 *         body = "단체 알림 내용",
 *         imageUrl = "https://example.com/images/group-notice.jpg"
 *     )
 * )
 */

import com.comdeply.comment.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class FcmService(
    private val firebaseSvc: FirebaseSvc,
    private val userRepository: UserRepository
) {
    // userId로 메시지 전송
//    @Transactional(readOnly = true)
//    fun sendMessageToUser(
//        userId: String,
//        message: FcmMessageDto,
//    ): String? =
//        userRepository
//            .findByUserId(userId)
//            .takeIf { it.isPresent }
//            ?.get()
//            ?.fcmToken
//            ?.let { token ->
//                log.info("Sending FCM message to user: $userId")
//                firebaseSvc.sendMessage(
//                    token = token,
//                    title = message.title,
//                    body = message.body,
//                    imageUrl = message.imageUrl,
//                    data = message.data,
//                )
//            }

    // 여러 사용자에게 메시지 전송
//    @Transactional(readOnly = true)
//    fun sendMessageToUsers(
//        userIds: List<String>,
//        message: FcmMessageDto,
//    ) {
//        val tokens =
//            userRepository
//                .findByUserIdIn(userIds)
//                .mapNotNull { it.fcmToken }
//
//        if (tokens.isNotEmpty()) {
//            firebaseSvc.sendMulticastMessage(
//                tokens = tokens,
//                title = message.title,
//                body = message.body,
//                imageUrl = message.imageUrl,
//                data = message.data,
//            )
//        }
//    }

    // fcmToken으로 직접 메시지 전송
//    fun sendMessageToToken(
//        token: String,
//        message: FcmMessageDto,
//    ): String =
//        firebaseSvc.sendMessage(
//            token = token,
//            title = message.title,
//            body = message.body,
//            imageUrl = message.imageUrl,
//            data = message.data,
//        )

    // 여러 fcmToken으로 직접 메시지 전송
//    fun sendMessageToTokens(
//        tokens: List<String>,
//        message: FcmMessageDto,
//    ) {
//        if (tokens.isNotEmpty()) {
//            firebaseSvc.sendMulticastMessage(
//                tokens = tokens,
//                title = message.title,
//                body = message.body,
//                imageUrl = message.imageUrl,
//                data = message.data,
//            )
//        }
//    }
}
