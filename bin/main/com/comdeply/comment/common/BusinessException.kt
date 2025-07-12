package com.comdeply.comment.common

import org.springframework.http.HttpStatus

class BusinessException(
    val code: String,
    override val message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST
) : RuntimeException(message)
