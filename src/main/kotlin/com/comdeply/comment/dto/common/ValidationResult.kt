package com.comdeply.comment.dto.common

/**
 * 공통 검증 결과 클래스
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String,
    val errorCode: String? = null,
    val details: Map<String, Any>? = null
) {
    companion object {
        fun success(message: String = "검증 성공"): ValidationResult {
            return ValidationResult(true, message)
        }

        fun error(message: String, errorCode: String? = null): ValidationResult {
            return ValidationResult(false, message, errorCode)
        }

        fun error(message: String, errorCode: String?, details: Map<String, Any>): ValidationResult {
            return ValidationResult(false, message, errorCode, details)
        }
    }
}
