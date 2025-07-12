package com.comdeply.comment.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "API 응답 공통 형식")
data class ApiResponse<T>(
    @Schema(description = "성공 여부", example = "true")
    val success: Boolean,

    @Schema(description = "응답 데이터")
    val data: T? = null,

    @Schema(description = "오류 메시지", example = "요청이 실패했습니다")
    val message: String? = null,

    @Schema(description = "응답 코드", example = "200")
    val code: Int? = null
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(success = true, data = data)
        }

        fun <T> success(data: T, message: String): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = message)
        }

        fun <T> error(message: String, code: Int? = null): ApiResponse<T> {
            return ApiResponse(success = false, message = message, code = code)
        }
    }
}

@Schema(description = "페이징 응답")
data class PageResponse<T>(
    @Schema(description = "데이터 목록")
    val content: List<T>,

    @Schema(description = "현재 페이지", example = "0")
    val page: Int,

    @Schema(description = "페이지 크기", example = "20")
    val size: Int,

    @Schema(description = "전체 요소 수", example = "100")
    val totalElements: Long,

    @Schema(description = "전체 페이지 수", example = "5")
    val totalPages: Int,

    @Schema(description = "첫 번째 페이지인지 여부", example = "true")
    val first: Boolean,

    @Schema(description = "마지막 페이지인지 여부", example = "false")
    val last: Boolean,

    @Schema(description = "다음 페이지가 있는지 여부", example = "true")
    val hasNext: Boolean
) {
    companion object {
        fun <T> of(page: org.springframework.data.domain.Page<T>): PageResponse<T> =
            PageResponse(
                content = page.content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                first = page.isFirst,
                last = page.isLast,
                hasNext = page.hasNext()
            )
    }
}

@Schema(description = "파일 업로드 응답")
data class FileUploadResponse(
    @Schema(description = "원본 파일명", example = "image.jpg")
    val originalFileName: String,

    @Schema(description = "저장된 파일명", example = "550e8400-e29b-41d4-a716-446655440000.jpg")
    val storedFileName: String,

    @Schema(description = "파일 경로", example = "2024/01/01/550e8400-e29b-41d4-a716-446655440000.jpg")
    val filePath: String,

    @Schema(description = "파일 크기 (바이트)", example = "1024000")
    val fileSize: Long,

    @Schema(description = "MIME 타입", example = "image/jpeg")
    val mimeType: String,

    @Schema(description = "파일 타입", example = "IMAGE")
    val fileType: String,

    @Schema(description = "파일 URL", example = "https://files.yourdomain.com/2024/01/01/550e8400-e29b-41d4-a716-446655440000.jpg")
    val fileUrl: String,

    @Schema(description = "썸네일 URL", example = "https://files.yourdomain.com/2024/01/01/thumb_550e8400-e29b-41d4-a716-446655440000.jpg")
    val thumbnailUrl: String?
)
