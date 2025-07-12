package com.comdeply.comment.app.web.svc

import com.comdeply.comment.entity.FileType
import org.apache.commons.io.FilenameUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO

@Service
class CloudflareStorageService(
    private val s3Client: S3Client
) {

    @Value("\${cloudflare.r2.bucket-name}")
    private lateinit var bucketName: String

    @Value("\${cloudflare.r2.public-url}")
    private lateinit var publicUrl: String

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
     * 파일 업로드 (이미지 리사이즈 및 썸네일 생성 포함)
     */
    fun uploadFile(file: MultipartFile): CloudflareUploadResult {
        validateFile(file)

        val originalFileName = file.originalFilename ?: throw IllegalArgumentException("파일명이 없습니다")
        val extension = FilenameUtils.getExtension(originalFileName).lowercase()
        val fileType = determineFileType(extension)

        val storedFileName = generateStoredFileName(extension)
        val datePath = generateDatePath()
        val filePath = "$datePath/$storedFileName"

        try {
            var fileBytes = file.bytes
            var thumbnailPath: String? = null

            // 이미지 파일인 경우 처리
            if (fileType == FileType.IMAGE) {
                fileBytes = processImage(fileBytes, extension)
                thumbnailPath = uploadThumbnail(fileBytes, datePath, storedFileName, extension)
            }

            // 메인 파일 업로드
            uploadToR2(filePath, fileBytes, file.contentType ?: "application/octet-stream")

            return CloudflareUploadResult(
                originalFileName = originalFileName,
                storedFileName = storedFileName,
                filePath = filePath,
                fileSize = fileBytes.size.toLong(),
                mimeType = file.contentType ?: "application/octet-stream",
                fileType = fileType,
                thumbnailPath = thumbnailPath,
                publicUrl = getPublicUrl(filePath),
                thumbnailUrl = thumbnailPath?.let { getPublicUrl(it) }
            )
        } catch (e: Exception) {
            throw RuntimeException("Cloudflare R2 업로드 중 오류가 발생했습니다", e)
        }
    }

    /**
     * 이미지 썸네일 업로드
     */
    private fun uploadThumbnail(imageBytes: ByteArray, datePath: String, originalFileName: String, extension: String): String {
        try {
            val originalImage = ImageIO.read(ByteArrayInputStream(imageBytes))
            if (originalImage == null) return ""

            val thumbnailImage = resizeImage(originalImage, thumbnailSize, thumbnailSize)
            val thumbnailFileName = "thumb_$originalFileName"
            val thumbnailPath = "$datePath/$thumbnailFileName"

            val thumbnailBytes = imageToBytes(thumbnailImage, extension)
            uploadToR2(thumbnailPath, thumbnailBytes, "image/$extension")

            return thumbnailPath
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * 이미지 처리 (리사이즈)
     */
    private fun processImage(imageBytes: ByteArray, extension: String): ByteArray {
        try {
            val originalImage = ImageIO.read(ByteArrayInputStream(imageBytes))
            if (originalImage == null) return imageBytes

            if (originalImage.width > maxWidth || originalImage.height > maxHeight) {
                val resizedImage = resizeImage(originalImage, maxWidth, maxHeight)
                return imageToBytes(resizedImage, extension)
            }

            return imageBytes
        } catch (e: Exception) {
            return imageBytes
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
     * BufferedImage를 바이트 배열로 변환
     */
    private fun imageToBytes(image: BufferedImage, format: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, format, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Cloudflare R2에 파일 업로드
     */
    private fun uploadToR2(key: String, data: ByteArray, contentType: String) {
        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .contentLength(data.size.toLong())
            .build()

        s3Client.putObject(request, RequestBody.fromBytes(data))
    }

    /**
     * 파일 삭제 (썸네일 포함)
     */
    fun deleteFile(filePath: String) {
        try {
            val deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(filePath)
                .build()

            s3Client.deleteObject(deleteRequest)

            // 썸네일도 삭제 시도
            val fileName = FilenameUtils.getName(filePath)
            val directory = FilenameUtils.getPath(filePath)
            val thumbnailPath = "${directory}thumb_$fileName"

            val thumbnailDeleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(thumbnailPath)
                .build()

            s3Client.deleteObject(thumbnailDeleteRequest)
        } catch (e: Exception) {
            // 삭제 실패 시 로그만 남기고 계속 진행
        }
    }

    /**
     * 파일 공개 URL 생성
     */
    fun getPublicUrl(filePath: String): String {
        return "$publicUrl/$filePath"
    }

    /**
     * 임시 액세스 URL 생성
     */
    fun generatePresignedUrl(filePath: String, expirationMinutes: Long = 60): String {
        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(filePath)
            .build()

        val presignRequest = s3Client.utilities().getUrl { builder ->
            builder.bucket(bucketName).key(filePath)
        }

        return presignRequest.toString()
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
}

data class CloudflareUploadResult(
    val originalFileName: String,
    val storedFileName: String,
    val filePath: String,
    val fileSize: Long,
    val mimeType: String,
    val fileType: FileType,
    val thumbnailPath: String?,
    val publicUrl: String,
    val thumbnailUrl: String?
)
