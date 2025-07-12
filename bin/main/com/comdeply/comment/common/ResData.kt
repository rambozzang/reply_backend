package com.comdeply.comment.common

import org.springframework.http.ResponseEntity

data class ResData<T>(
    var success: Boolean = true,
    val message: String? = null,
    var data: T? = null,
    val error: String? = null
) {
    companion object {
        fun <T> success(data: T): ResponseEntity<ResData<T>> = ResponseEntity.ok(ResData(success = true, data = data))

        fun <T> success(
            data: T,
            msg: String
        ): ResponseEntity<ResData<T>> = ResponseEntity.ok(ResData(success = true, data = data, message = msg))

        fun fail(msg: String): ResponseEntity<ResData<Nothing>> = ResponseEntity.ok(ResData(success = false, error = msg))

        fun error(msg: String): ResponseEntity<ResData<Nothing>> = ResponseEntity.ok(ResData(success = false, error = msg))

        fun <T> fail(
            data: T,
            msg: String
        ): ResponseEntity<ResData<T>> = ResponseEntity.ok(ResData(success = false, data = data, error = msg))
    }
}
