package com.evorsio.mybox.file;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "files")
@Comment("文件元数据表，对应 MinIO 中的对象信息")
public class File {
    @Id
    @GeneratedValue
    @Comment("文件记录唯一标识（数据库主键）")
    private UUID id;

    @Column
    @Comment("所属文件夹 ID（null 表示未分类）")
    private UUID folderId;

    @Column(nullable = false)
    @Comment("用户上传时的原始文件名")
    private String originalFileName;

    @Column(nullable = false)
    @Comment("MinIO 中的对象名称（Object Name，通常为 UUID 或路径）")
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
    @Comment("文件所属用户 ID（与用户系统关联）")
    private UUID ownerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Comment("文件自定义元数据，存储为 JSON 格式")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Column(nullable = false, updatable = false)
    @Comment("文件创建时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("文件最后更新时间")
    private LocalDateTime updatedAt;

    @Comment("文件软删除时间")
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    @Comment("文件哈希值")
    private String fileHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("文件状态：ACTIVE / DELETED")
    @Builder.Default
    private FileStatus status = FileStatus.UPLOADING;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== 业务方法 ==========

    /**
     * 判断是否已删除
     */
    public boolean isDeleted() {
        return status == FileStatus.DELETED;
    }

    /**
     * 判断是否活跃
     */
    public boolean isActive() {
        return status == FileStatus.ACTIVE;
    }

    /**
     * 判断是否上传中
     */
    public boolean isUploading() {
        return status == FileStatus.UPLOADING;
    }

    /**
     * 判断是否上传失败
     */
    public boolean isFailed() {
        return status == FileStatus.FAILED;
    }

    /**
     * 判断是否被封禁
     */
    public boolean isBlocked() {
        return status == FileStatus.BLOCKED;
    }

    /**
     * 判断是否已物理删除
     */
    public boolean isPurged() {
        return status == FileStatus.PURGED;
    }

    /**
     * 软删除文件
     */
    public void markAsDeleted() {
        this.status = FileStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 恢复文件
     */
    public void restore() {
        this.status = FileStatus.ACTIVE;
        this.deletedAt = null;
    }

    /**
     * 标记为上传失败
     */
    public void markAsFailed() {
        this.status = FileStatus.FAILED;
    }

    /**
     * 封禁文件
     */
    public void block() {
        this.status = FileStatus.BLOCKED;
    }

    /**
     * 标记为物理删除
     */
    public void markAsPurged() {
        this.status = FileStatus.PURGED;
    }
}
