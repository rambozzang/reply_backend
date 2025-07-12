package com.comdeply.comment.app.admin.svc.vo

data class UsernameCheckResponse(
    val username: String,
    val exists: Boolean,
    val message: String
)
