package com.comdeply.comment.app.web.svc

import com.comdeply.comment.entity.FileType
import org.apache.commons.io.FilenameUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO

@Service
class FileUploadService(
    private val cloudflareStorageService: CloudflareStorageService,
    private val localFileService: LocalFileService
) {

    @Value("\${file.upload.storage-type}")
    private lateinit var storageType: String

    @Value("\${file.upload.max-size}")
    private lateinit var maxSize: String

    @Value("\${file.upload.allowed-types}")
    private lateinit var allowedTypes: String

    @Value("\${file.upload.image.max-width}")
    private var maxWidth: Int = 1920

    @Value("\${file.upload.image.max-height}")
    private var maxHeight: Int = 1080

    @Value("\${file.upload.image.thumbnail-size}")
    private var thumbnailSize: Int = 200

    /**
     * 파일 업로드 (설정에 따라 Cloudflare 또는 로컬 저장소 사용)
     */
    fun uploadFile(file: MultipartFile): FileUploadResult {
        return when (storageType.lowercase()) {
            "cloudflare" -> {
                val result = cloudflareStorageService.uploadFile(file)
                FileUploadResult(
                    originalFileName = result.originalFileName,
                    storedFileName = result.storedFileName,
                    filePath = result.filePath,
                    fileSize = result.fileSize,
                    mimeType = result.mimeType,
                    fileType = result.fileType,
                    thumbnailPath = result.thumbnailPath,
                    fileUrl = result.publicUrl,
                    thumbnailUrl = result.thumbnailUrl
                )
            }
            "local" -> {
                val result = localFileService.uploadFile(file)
                FileUploadResult(
                    originalFileName = result.originalFileName,
                    storedFileName = result.storedFileName,
                    filePath = result.filePath,
                    fileSize = result.fileSize,
                    mimeType = result.mimeType,
                    fileType = result.fileType,
                    thumbnailPath = result.thumbnailPath,
                    fileUrl = getFileUrl(result.filePath),
                    thumbnailUrl = result.thumbnailPath?.let { getFileUrl(it) }
                )
            }
            else -> throw IllegalArgumentException("지원하지 않는 스토리지 타입: $storageType")
        }
    }

    /**
     * 파일 유효성 검증
     */
    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw IllegalArgumentException("빈 파일입니다")
        }

        val maxSizeBytes = parseSize(maxSize)
        if (file.size > maxSizeBytes) {
            throw IllegalArgumentException("파일 크기가 너무 큽니다. 최대 크기: $maxSize")
        }

        val extension = FilenameUtils.getExtension(file.originalFilename ?: "").lowercase()
        val allowedExtensions = allowedTypes.split(",").map { it.trim() }

        if (extension !in allowedExtensions) {
            throw IllegalArgumentException("허용되지 않는 파일 형식입니다. 허용된 형식: $allowedTypes")
        }
    }

    /**
     * 파일 확장자로 파일 타입 결정
     */
    private fun determineFileType(extension: String): FileType {
        return when (extension) {
            in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> FileType.IMAGE
            in listOf("mp4", "avi", "mov", "wmv", "flv", "webm") -> FileType.VIDEO
            in listOf("pdf", "doc", "docx", "txt", "hwp") -> FileType.DOCUMENT
            else -> FileType.OTHER
        }
    }

    /**
     * 저장용 파일명 생성 (UUID 기반)
     */
    private fun generateStoredFileName(extension: String): String {
        return "${UUID.randomUUID()}.$extension"
    }

    /**
     * 날짜 기반 디렉토리 경로 생성
     */
    private fun generateDatePath(): String {
        val now = LocalDateTime.now()
        return now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    }

    /**
     * 이미지 썸네일 생성
     */
    private fun createThumbnail(originalPath: Path, uploadDir: Path, storedFileName: String): String {
        try {
            val originalImage = ImageIO.read(originalPath.toFile())
            if (originalImage == null) return ""

            val thumbnailImage = resizeImage(originalImage, thumbnailSize, thumbnailSize)
            val thumbnailFileName = "thumb_$storedFileName"
            val thumbnailPath = uploadDir.resolve(thumbnailFileName)

            val extension = FilenameUtils.getExtension(storedFileName)
            ImageIO.write(thumbnailImage, extension, thumbnailPath.toFile())

            return "${generateDatePath()}/$thumbnailFileName"
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * 필요시 이미지 리사이즈
     */
    private fun resizeImageIfNeeded(filePath: Path) {
        try {
            val originalImage = ImageIO.read(filePath.toFile())
            if (originalImage == null) return

            if (originalImage.width > maxWidth || originalImage.height > maxHeight) {
                val resizedImage = resizeImage(originalImage, maxWidth, maxHeight)
                val extension = FilenameUtils.getExtension(filePath.fileName.toString())
                ImageIO.write(resizedImage, extension, filePath.toFile())
            }
        } catch (e: Exception) {
            // 리사이징 실패 시 원본 파일 유지
        }
    }

    /**
     * 이미지 리사이즈 (비율 유지)
     */
    private fun resizeImage(originalImage: BufferedImage, maxWidth: Int, maxHeight: Int): BufferedImage {
        val originalWidth = originalImage.width
        val originalHeight = originalImage.height

        val ratio = minOf(
            maxWidth.toDouble() / originalWidth,
            maxHeight.toDouble() / originalHeight
        )

        val newWidth = (originalWidth * ratio).toInt()
        val newHeight = (originalHeight * ratio).toInt()

        val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val graphics: Graphics2D = resizedImage.createGraphics()

        graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null)
        graphics.dispose()

        return resizedImage
    }

    /**
     * 크기 문자열을 바이트로 변환
     */
    private fun parseSize(sizeStr: String): Long {
        val regex = """(\d+)([KMGT]?B)""".toRegex(RegexOption.IGNORE_CASE)
        val matchResult = regex.find(sizeStr.replace(" ", ""))
            ?: throw IllegalArgumentException("잘못된 크기 형식: $sizeStr")

        val (number, unit) = matchResult.destructured
        val bytes = number.toLong()

        return when (unit.uppercase()) {
            "B" -> bytes
            "KB" -> bytes * 1024
            "MB" -> bytes * 1024 * 1024
            "GB" -> bytes * 1024 * 1024 * 1024
            "TB" -> bytes * 1024 * 1024 * 1024 * 1024
            else -> bytes
        }
    }

    /**
     * 파일 삭제
     */
    fun deleteFile(filePath: String) {
        when (storageType.lowercase()) {
            "cloudflare" -> cloudflareStorageService.deleteFile(filePath)
            "local" -> localFileService.deleteFile(filePath)
        }
    }

    /**
     * 파일 URL 생성
     */
    fun getFileUrl(filePath: String): String {
        return "/api/files/$filePath"
    }
}

data class FileUploadResult(
    val originalFileName: String,
    val storedFileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val fileType: FileType,
    val thumbnailPath: String?,
    val fileUrl: String,
    val thumbnailUrl: String?
)
