package com.comdeply.comment.common

import org.springframework.data.domain.Page

// 페이징 응답을 위한 데이터 클래스
data class PageResponse<T>(
    val content: List<T>,
    val currentPage: Int,
    val totalPages: Int,
    val totalCount: Long,
    val isLast: Boolean = false,
    val size: Int
) {
    companion object {
        fun <T> of(page: Page<T>): PageResponse<T> =
            PageResponse(
                content = page.content,
                currentPage = page.number,
                totalPages = page.totalPages,
                totalCount = page.totalElements,
                isLast = page.isLast,
                size = page.size
            )
    }
}
