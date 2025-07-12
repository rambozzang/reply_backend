package com.comdeply.comment.common

data class ErrorResponse<T>(
    var success: Boolean = true,
    val message: String? = null,
    var data: T? = null,
    val error: String? = null
)
