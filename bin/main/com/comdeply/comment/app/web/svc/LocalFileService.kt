package com.comdeply.comment.app.web.svc

import com.comdeply.comment.entity.FileType
import org.apache.commons.io.FilenameUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO

@Service
class LocalFileService {

    @Value("\${file.upload.path}")
    private lateinit var uploadPath: String

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

    fun uploadFile(file: MultipartFile): LocalFileUploadResult {
        validateFile(file)

        val originalFileName = file.originalFilename ?: throw IllegalArgumentException("파일명이 없습니다")
        val extension = FilenameUtils.getExtension(originalFileName).lowercase()
        val fileType = determineFileType(extension)

        val storedFileName = generateStoredFileName(extension)
        val datePath = generateDatePath()
        val uploadDir = Paths.get(uploadPath, datePath)

        // 디렉토리 생성
        Files.createDirectories(uploadDir)

        val filePath = uploadDir.resolve(storedFileName)
        val relativePath = "$datePath/$storedFileName"

        try {
            // 파일 저장
            Files.copy(file.inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)

            var thumbnailPath: String? = null

            // 이미지 파일인 경우 썸네일 생성
            if (fileType == FileType.IMAGE) {
                thumbnailPath = createThumbnail(filePath, uploadDir, storedFileName)

                // 이미지 리사이징 (필요한 경우)
                resizeImageIfNeeded(filePath)
            }

            return LocalFileUploadResult(
                originalFileName = originalFileName,
                storedFileName = storedFileName,
                filePath = relativePath,
                fileSize = file.size,
                mimeType = file.contentType ?: "application/octet-stream",
                fileType = fileType,
                thumbnailPath = thumbnailPath
            )
        } catch (e: IOException) {
            throw RuntimeException("파일 업로드 중 오류가 발생했습니다", e)
        }
    }

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

    private fun determineFileType(extension: String): FileType {
        return when (extension) {
            in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> FileType.IMAGE
            in listOf("mp4", "avi", "mov", "wmv", "flv", "webm") -> FileType.VIDEO
            in listOf("pdf", "doc", "docx", "txt", "hwp") -> FileType.DOCUMENT
            else -> FileType.OTHER
        }
    }

    private fun generateStoredFileName(extension: String): String {
        return "${UUID.randomUUID()}.$extension"
    }

    private fun generateDatePath(): String {
        val now = LocalDateTime.now()
        return now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
    }

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

    fun deleteFile(filePath: String) {
        try {
            val fullPath = Paths.get(uploadPath, filePath)
            Files.deleteIfExists(fullPath)

            // 썸네일도 삭제 시도
            val thumbnailPath = fullPath.parent.resolve("thumb_${fullPath.fileName}")
            Files.deleteIfExists(thumbnailPath)
        } catch (e: Exception) {
            // 삭제 실패 시 로그만 남기고 계속 진행
        }
    }
}

data class LocalFileUploadResult(
    val originalFileName: String,
    val storedFileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val fileType: FileType,
    val thumbnailPath: String?
)
