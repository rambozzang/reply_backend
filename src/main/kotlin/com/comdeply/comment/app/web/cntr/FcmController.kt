package com.comdeply.comment.app.web.cntr
import com.comdeply.comment.app.web.svc.FcmService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "FCM", description = "Firebase Cloud Messaging API")
@RestController
@RequestMapping("/fcm")
class FcmController(
    private val fcmService: FcmService
) {
//    @Operation(summary = "FCM 메시지 전송 (userId)")
//    @PostMapping("/send/user/{userId}")
//    fun sendMessageToUser(
//        @PathVariable userId: String,
//        @RequestBody message: FcmMessage,
//    ): ResponseEntity<String> {
//        val messageId = fcmService.sendMessageToUser(userId, message)
//        return if (messageId != null) {
//            ResponseEntity.ok(messageId)
//        } else {
//            ResponseEntity.notFound().build()
//        }
//    }

//    @Operation(summary = "FCM 메시지 전송 (FCM 토큰)")
//    @PostMapping("/send/token/{token}")
//    fun sendMessageToToken(
//        @PathVariable token: String,
//        @RequestBody message: FcmMessage,
//    ): ResponseEntity<String> {
//        val messageId = fcmService.sendMessageToToken(token, message)
//        return ResponseEntity.ok(messageId)
//    }

//    @Operation(summary = "FCM 메시지 전송 (다중 사용자)")
//    @PostMapping("/send/users")
//    fun sendMessageToUsers(
//        @RequestBody request: BulkMessageRequest,
//    ): ResponseEntity<Unit> {
//        fcmService.sendMessageToUsers(request.userIds, request.message)
//        return ResponseEntity.ok().build()
//    }

//    @Operation(summary = "FCM 메시지 전송 (다중 토큰)")
//    @PostMapping("/send/tokens")
//    fun sendMessageToTokens(
//        @RequestBody request: BulkTokenMessageRequest,
//    ): ResponseEntity<Unit> {
//        fcmService.sendMessageToTokens(request.tokens, request.message)
//        return ResponseEntity.ok().build()
//    }

//    @PostMapping("/test")
//    fun testFcm(
//        @RequestParam token: String,
//        @RequestParam title: String = "테스트 제목",
//        @RequestParam body: String = "테스트 메시지"
//    ): ResponseEntity<Map<String, String>> {
//        val messageId = fcmService.sendMessage(token, title, body)
//        return ResponseEntity.ok(mapOf(
//            "status" to "success",
//            "messageId" to messageId
//        ))
//    }
}

// data class BulkMessageRequest(
//    val userIds: List<String>,
//    val message: FcmMessageDto,
// )
//
// data class BulkTokenMessageRequest(
//    val tokens: List<String>,
//    val message: FcmMessageDto,
// )
