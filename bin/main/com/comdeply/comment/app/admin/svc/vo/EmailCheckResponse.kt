package com.comdeply.comment.app.admin.svc.vo

data class EmailCheckResponse(
    val email: String,
    val exists: Boolean,
    val message: String
)
