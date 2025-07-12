package com.comdeply.comment.app.web.svc

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service

@Service
@Configuration
class FirebaseSvc {
    private val logger = LoggerFactory.getLogger(this::class.java)
//    private val firebaseMessaging: FirebaseMessaging

    init {
//        try {
//            val googleCredentials =
//                GoogleCredentials
//                    .fromStream(ClassPathResource("firebase-key.json").inputStream)
//
//            val firebaseOptions =
//                FirebaseOptions
//                    .builder()
//                    .setCredentials(googleCredentials)
//                    .build()
//
//            if (FirebaseApp.getApps().isEmpty()) {
//                FirebaseApp.initializeApp(firebaseOptions)
//            }
//
//            firebaseMessaging = FirebaseMessaging.getInstance()
//        } catch (e: IOException) {
//            throw RuntimeException("Firebase 초기화 실패", e)
//        }
    }

    // 단일 토큰으로 메시지 전송
//    fun sendMessage(
//        token: String,
//        title: String,
//        body: String,
//        imageUrl: String? = null,
//        data: Map<String, String> = emptyMap(),
//    ): String {
//        logger.debug("Sending FCM message to token: $token")
//
//        val notification =
//            Notification
//                .builder()
//                .setTitle(title)
//                .setBody(body)
//                .apply {
//                    imageUrl?.takeIf { it.startsWith("https://") }?.let {
//                        setImage(it)
//                    }
//                }.build()
//        val messageBuilder =
//            Message
//                .builder()
//                .setToken(token)
//                .setNotification(notification)
//                .putAllData(data)
//
//        return try {
//            val result = firebaseMessaging.send(messageBuilder.build())
//            logger.info("Successfully sent message: $result")
//            result
//        } catch (e: Exception) {
//            logger.error("Failed to send FCM message", e)
//            throw Exception("메시지 전송 실패", e)
//        }
//    }

    // 여러 토큰으로 메시지 전송
//    fun sendMulticastMessage(
//        tokens: List<String>,
//        title: String,
//        body: String,
//        imageUrl: String? = null,
//        data: Map<String, String> = emptyMap(),
//    ): BatchResponse {
//        val notificationBuilder =
//            Notification
//                .builder()
//                .setTitle(title)
//                .setBody(body)
//
//        // 이미지 URL이 있는 경우 추가
//        imageUrl?.let {
//            notificationBuilder.setImage(it)
//        }
//
//        val message =
//            MulticastMessage
//                .builder()
//                .addAllTokens(tokens)
//                .setNotification(notificationBuilder.build())
//                .putAllData(data)
//                .build()
//
//        return firebaseMessaging.sendEachForMulticast(message)
//    }
}
