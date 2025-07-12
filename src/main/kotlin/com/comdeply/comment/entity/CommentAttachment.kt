package com.comdeply.comment.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "comment_attachments")
data class CommentAttachment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    val comment: Comment,

    @Column(nullable = false)
    val originalFileName: String,

    @Column(nullable = false)
    val storedFileName: String,

    @Column(nullable = false)
    val filePath: String,

    @Column(nullable = false)
    val fileSize: Long,

    @Column(nullable = false)
    val mimeType: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val fileType: FileType,

    @Column
    val thumbnailPath: String? = null,

    @Column(nullable = false)
    val fileUrl: String,

    @Column
    val thumbnailUrl: String? = null,

    @Column(nullable = false)
    val fileName: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class FileType {
    IMAGE, VIDEO, DOCUMENT, OTHER
}
