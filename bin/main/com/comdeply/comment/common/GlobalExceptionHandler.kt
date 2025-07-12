package com.comdeply.comment.common

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse<Nothing>> {
        log.error("Unhandled exception occurred", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    success = false,
                    message = "서버 내부 오류가 발생했습니다.",
                    data = null,
                    error = e.message ?: "서버 내부 오류가 발생했습니다."
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse<Nothing>> {
//        val errorMessage =
//            e.bindingResult.fieldErrors
//                .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        val errorMessage =
            e
                .bindingResult
                .fieldErrors
                .joinToString(", ") { "${it.defaultMessage}" }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                ErrorResponse(
                    success = false,
                    message = errorMessage,
                    data = null,
                    error = errorMessage
                )
            )
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse<Nothing>> =
        ResponseEntity
            .status(e.status)
            .body(
                ErrorResponse(
                    success = false,
                    message = e.message,
                    data = null,
                    error = e.message
                )
            )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse<Nothing>> =
        ResponseEntity
            .status(e.message?.let { HttpStatus.BAD_REQUEST } ?: HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    success = false,
                    message = e.message,
                    data = null,
                    error = e.message
                )
            )
}
