package com.comdeply.comment.app.web.cntr

import com.comdeply.comment.dto.ApiResponse
import com.comdeply.comment.utils.EmailService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@Tag(name = "문의", description = "상담 문의 API")
@RestController
@RequestMapping("/contact")
@CrossOrigin(originPatterns = ["*"], allowedHeaders = ["*"])
class ContactController(
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(ContactController::class.java)

    @Operation(summary = "상담 신청", description = "무료 상담 신청을 처리합니다.")
    @PostMapping("/consultation")
    @PreAuthorize("permitAll()")
    fun submitConsultation(
        @Valid @RequestBody
        request: ConsultationRequest
    ): ResponseEntity<ApiResponse<String>> {
        return try {
            logger.info("상담 신청 접수: 회사={}, 이름={}, 이메일={}", request.company, request.name, request.email)
            
            // 이메일 전송
            emailService.sendContactEmail(
                customerName = request.name,
                customerEmail = request.email,
                customerPhone = request.phone,
                inquiryType = "상담 신청",
                subject = "[CommentPly] 새로운 상담 신청 - ${request.company}",
                content = buildString {
                    appendLine("=== 상담 신청 정보 ===")
                    appendLine("회사명: ${request.company}")
                    appendLine("담당자: ${request.name}")
                    appendLine("이메일: ${request.email}")
                    appendLine("전화번호: ${request.phone}")
                    if (!request.website.isNullOrBlank()) {
                        appendLine("웹사이트: ${request.website}")
                    }
                    if (!request.visitors.isNullOrBlank()) {
                        appendLine("월 방문자 수: ${request.visitors}")
                    }
                    if (!request.features.isNullOrEmpty()) {
                        appendLine("관심 기능: ${request.features.joinToString(", ")}")
                    }
                    appendLine()
                    appendLine("=== 문의 내용 ===")
                    appendLine(request.message)
                }
            )
            
            logger.info("상담 신청 처리 완료: {}", request.email)
            
            ResponseEntity.ok(
                ApiResponse.success(
                    data = "상담 신청이 성공적으로 접수되었습니다. 빠른 시일 내에 연락드리겠습니다.",
                    message = "상담 신청 완료"
                )
            )
        } catch (e: Exception) {
            logger.error("상담 신청 처리 중 오류 발생", e)
            ResponseEntity.badRequest().body(
                ApiResponse.error<String>(
                    message = "상담 신청 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                )
            )
        }
    }
}

data class ConsultationRequest(
    @field:NotBlank(message = "회사명은 필수입니다")
    @field:Size(max = 100, message = "회사명은 100자 이하여야 합니다")
    val company: String,

    @field:NotBlank(message = "담당자명은 필수입니다")
    @field:Size(max = 50, message = "담당자명은 50자 이하여야 합니다")
    val name: String,

    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,

    @field:NotBlank(message = "전화번호는 필수입니다")
    @field:Size(max = 20, message = "전화번호는 20자 이하여야 합니다")
    val phone: String,

    @field:Size(max = 200, message = "웹사이트 URL은 200자 이하여야 합니다")
    val website: String?,

    @field:Size(max = 50, message = "월 방문자 수는 50자 이하여야 합니다")
    val visitors: String?,

    @field:Size(max = 500, message = "관심 기능은 500자 이하여야 합니다")
    val features: List<String>?,

    @field:NotBlank(message = "문의 내용은 필수입니다")
    @field:Size(max = 2000, message = "문의 내용은 2000자 이하여야 합니다")
    val message: String
)
