package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.dto.CommentResponse
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class WebSocketController(
    private val messagingTemplate: SimpMessagingTemplate
) {

    @MessageMapping("/comment.new")
    @SendTo("/topic/comments")
    fun newComment(commentDto: CommentResponse): CommentResponse {
        return commentDto
    }

    @MessageMapping("/comment.update")
    @SendTo("/topic/comments")
    fun updateComment(commentDto: CommentResponse): CommentResponse {
        return commentDto
    }

    @MessageMapping("/comment.delete")
    @SendTo("/topic/comments")
    fun deleteComment(commentId: Long): Map<String, Any> {
        return mapOf(
            "type" to "DELETE",
            "commentId" to commentId
        )
    }

    fun broadcastNewComment(siteId: String, pageId: String, comment: CommentResponse) {
        messagingTemplate.convertAndSend("/topic/comments/$siteId/$pageId", comment)
    }

    fun broadcastCommentUpdate(siteId: String, pageId: String, comment: CommentResponse) {
        messagingTemplate.convertAndSend("/topic/comments/$siteId/$pageId", comment)
    }

    fun broadcastCommentDelete(siteId: String, pageId: String, commentId: Long) {
        val message = mapOf(
            "type" to "DELETE",
            "commentId" to commentId
        )
        messagingTemplate.convertAndSend("/topic/comments/$siteId/$pageId", message)
    }
}
