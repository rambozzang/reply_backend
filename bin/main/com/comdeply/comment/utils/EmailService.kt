package com.comdeply.comment.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.nio.charset.StandardCharsets

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: TemplateEngine,
    @Value("\${spring.mail.username}") private val fromEmail: String
) {
    fun sendFindIdEmail(
        toEmail: String,
        userId: String
    ) {
        val context = Context()
        context.setVariable("userId", userId)

        val htmlContent = templateEngine.process("find-id-template", context)

        val message = mailSender.createMimeMessage()
        val helper =
            MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
            )

        helper.setFrom(fromEmail)
        helper.setTo(toEmail)
        helper.setSubject("[CommentPly] 아이디 찾기 결과")
        helper.setText(htmlContent, true)

        mailSender.send(message)
    }

    fun sendResetPasswordEmail(
        toEmail: String,
        userName: String,
        newPassword: String
    ) {
        val context = Context()
        context.setVariable("userName", userName)
        context.setVariable("newPassword", newPassword)

        val htmlContent = templateEngine.process("reset-password-template", context)

        val message = mailSender.createMimeMessage()
        val helper =
            MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
            )

        helper.setFrom(fromEmail)
        helper.setTo(toEmail)
        helper.setSubject("[CommentPly] 임시 비밀번호 안내")
        helper.setText(htmlContent, true)

        mailSender.send(message)
    }

    // 견적 이메일 발송 메소드 추가
    fun sendQuotationEmail(
        toEmail: String,
        quotationTitle: String,
        quotationContent: String,
        quotationCost: String,
        senderCompany: String
    ) {
        val context = Context()
        context.setVariable("quotationTitle", quotationTitle)
        context.setVariable("quotationContent", quotationContent)
        context.setVariable("quotationCost", quotationCost)
        context.setVariable("senderCompany", senderCompany)

        val htmlContent = templateEngine.process("quotation-template", context)

        val message = mailSender.createMimeMessage()
        val helper =
            MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
            )

        helper.setFrom(fromEmail)
        helper.setTo(toEmail)
        helper.setSubject("[CommentPly] 상담 견적 안내: $quotationTitle")
        helper.setText(htmlContent, true)

        mailSender.send(message)
    }

    // 온라인 문의 이메일 발송 메소드 추가
    fun sendContactEmail(
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        inquiryType: String,
        subject: String,
        content: String
    ) {
        val message = mailSender.createMimeMessage()
        val helper =
            MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
            )

        helper.setFrom(fromEmail)
        helper.setTo("support@commentply.com") // CommentPly 지원 이메일
        helper.setSubject(subject)
        helper.setText(content, false) // 텍스트 이메일로 변경

        mailSender.send(message)
    }
}
