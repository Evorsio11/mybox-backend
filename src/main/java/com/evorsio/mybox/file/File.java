package com.evorsio.mybox.file;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件实体 - 管理 MinIO 中的实际存储文件
 * <p>
 * 多个 FileRecord 记录可以引用同一个 File（内容去重）
 * 只有当所有引用都被删除时，才能安全删除实际文件
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "files_storage", indexes = {
        @Index(name = "idx_file_hash", columnList = "fileHash"),
        @Index(name = "idx_file_bucket_object", columnList = "bucket, objectName")
})
@Comment("文件存储表，管理 MinIO 中的实际文件存储，支持内容去重")
public class File {

    @Id
    @GeneratedValue
    @Comment("文件唯一标识（数据库主键）")
    private UUID id;

    @Column(nullable = false, unique = true)
    @Comment("文件内容的 SHA-256 哈希值，用于内容去重")
    private String fileHash;

    @Column(nullable = false)
    @Comment("MinIO 中的对象名称（Object Name）")
    private String objectName;

    @Column(nullable = false)
    @Comment("MinIO 存储桶名称（Bucket Name）")
    private String bucket;

    @Column(nullable = false)
    @Comment("文件 MIME 类型，如 image/png、application/pdf")
    private String contentType;

    @Column(nullable = false)
    @Comment("文件大小，单位：字节")
    private Long size;

    @Column(nullable = false)
    @Comment("引用此文件的 FileRecord 记录数量")
    @Builder.Default
    private Integer referenceCount = 1;

    @Column(nullable = false, updatable = false)
    @Comment("文件创建时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("文件最后更新时间")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (referenceCount == null) {
            referenceCount = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== 业务方法 ==========

    /**
     * 增加引用计数（当新的 FileRecord 记录引用此文件时调用）
     */
    public void incrementReferenceCount() {
        this.referenceCount++;
    }

    /**
     * 减少引用计数（当 FileRecord 记录被删除时调用）
     *
     * @return 新的引用计数
     */
    public int decrementReferenceCount() {
        if (this.referenceCount > 0) {
            this.referenceCount--;
        }
        return this.referenceCount;
    }

    /**
     * 检查是否可以安全删除文件（无任何引用）
     */
    public boolean canBeDeleted() {
        return this.referenceCount <= 0;
    }

    /**
     * 检查是否有多个文件记录引用（共享存储）
     */
    public boolean isShared() {
        return this.referenceCount > 1;
    }
}
