package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.app.web.svc.FileUploadService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths

@Tag(name = "파일 관리", description = "파일 업로드, 다운로드, 삭제 API")
@RestController
@RequestMapping("/files")
class FileController(
    private val fileUploadService: FileUploadService
) {

    @Value("\${file.upload.path}")
    private lateinit var uploadPath: String

    @Operation(
        summary = "파일 업로드",
        description = "이미지 파일을 업로드합니다. 지원 형식: JPG, PNG, GIF, WebP (최대 10MB)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "파일 업로드 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """
                        {
                            "success": true,
                            "originalFileName": "image.jpg",
                            "storedFileName": "uuid.jpg",
                            "filePath": "2024/01/01/uuid.jpg",
                            "fileSize": 1024000,
                            "mimeType": "image/jpeg",
                            "fileType": "IMAGE",
                            "fileUrl": "https://files.yourdomain.com/2024/01/01/uuid.jpg",
                            "thumbnailUrl": "https://files.yourdomain.com/2024/01/01/thumb_uuid.jpg"
                        }
                        """
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (파일 크기 초과, 지원하지 않는 형식 등)",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """
                        {
                            "success": false,
                            "error": "파일 크기가 너무 큽니다. 최대 크기: 10MB"
                        }
                        """
                            )
                        ]
                    )
                ]
            )
        ]
    )
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @Parameter(description = "업로드할 이미지 파일", required = true)
        @RequestParam("file")
        file: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val result = fileUploadService.uploadFile(file)

            val response: Map<String, Any> = mapOf(
                "success" to true,
                "originalFileName" to result.originalFileName,
                "storedFileName" to result.storedFileName,
                "filePath" to result.filePath,
                "fileSize" to result.fileSize,
                "mimeType" to result.mimeType,
                "fileType" to result.fileType.name,
                "fileUrl" to result.fileUrl,
                "thumbnailUrl" to (result.thumbnailUrl ?: "")
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            val errorResponse: Map<String, Any> = mapOf(
                "success" to false,
                "error" to (e.message ?: "파일 업로드 중 오류가 발생했습니다")
            )
            ResponseEntity.badRequest().body(errorResponse)
        }
    }

    @Operation(
        summary = "파일 다운로드",
        description = "업로드된 파일을 다운로드합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "파일 다운로드 성공"),
            ApiResponse(responseCode = "404", description = "파일을 찾을 수 없음")
        ]
    )
    @GetMapping("/**")
    fun serveFile(
        @Parameter(description = "파일 경로", required = true)
        @RequestParam
        filePath: String
    ): ResponseEntity<Resource> {
        return try {
            val fullPath = Paths.get(uploadPath, filePath)
            val resource = FileSystemResource(fullPath)

            if (!resource.exists() || !resource.isReadable) {
                return ResponseEntity.notFound().build()
            }

            val contentType = Files.probeContentType(fullPath) ?: "application/octet-stream"

            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${resource.filename}\"")
                .body(resource)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "파일 삭제",
        description = "업로드된 파일을 삭제합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "파일 삭제 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """{"success": true, "message": "파일이 삭제되었습니다"}"""
                            )
                        ]
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "파일 삭제 실패",
                content = [
                    Content(
                        mediaType = "application/json",
                        examples = [
                            ExampleObject(
                                value = """{"success": false, "error": "파일 삭제 중 오류가 발생했습니다"}"""
                            )
                        ]
                    )
                ]
            )
        ]
    )
    @DeleteMapping("/{filePath}")
    fun deleteFile(
        @Parameter(description = "삭제할 파일 경로", required = true)
        @PathVariable
        filePath: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            fileUploadService.deleteFile(filePath)
            ResponseEntity.ok(mapOf("success" to true, "message" to "파일이 삭제되었습니다"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf(
                    "success" to false,
                    "error" to (e.message ?: "파일 삭제 중 오류가 발생했습니다")
                )
            )
        }
    }
}
