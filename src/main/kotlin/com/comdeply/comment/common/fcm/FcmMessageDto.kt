package com.comdeply.comment.common.fcm

data class FcmMessage(
    val title: String,
    val body: String,
    val imageUrl: String? = null,
    val data: Map<String, String> = emptyMap()
)
