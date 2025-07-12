package com.comdeply.comment.utils

enum class PlanType {
    FREE,
    STARTER,
    PRO,
    PREMIUM,
    ENTERPRISE;

    companion object {
        fun fromString(value: String): PlanType? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
