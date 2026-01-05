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
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件记录实体 - 表示用户视角的文件
 * <p>
 * 多个 FileRecord 可以引用同一个 File（内容去重）
 * FileRecord 记录用户可见的文件信息，File 管理实际存储
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "file_records", indexes = {
        @Index(name = "idx_file_record_owner_folder", columnList = "ownerId, folderId"),
        @Index(name = "idx_file_record_owner_status", columnList = "ownerId, status"),
        @Index(name = "idx_file_record_file", columnList = "fileId")
})
@Comment("文件记录表，表示用户视角的文件元数据")
public class FileRecord {
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

    // ========== 文件引用 ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fileId", nullable = false, foreignKey = @ForeignKey(name = "fk_file_record_file"))
    @Comment("关联的实际文件（多个记录可共享同一文件）")
    private File file;

    @Column(name = "fileId", insertable = false, updatable = false)
    @Comment("文件 ID（用于查询优化）")
    private UUID fileId;

    // ========== 文件元数据 ==========

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("文件状态：ACTIVE / DELETED / UPLOADING / FAILED / BLOCKED / PURGED")
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

    // ========== 文件访问方法 ==========

    /**
     * 获取对象名称
     */
    public String getObjectName() {
        return file != null ? file.getObjectName() : null;
    }

    /**
     * 获取存储桶名称
     */
    public String getBucket() {
        return file != null ? file.getBucket() : null;
    }

    /**
     * 获取文件 MIME 类型
     */
    public String getContentType() {
        return file != null ? file.getContentType() : null;
    }

    /**
     * 获取文件大小
     */
    public Long getSize() {
        return file != null ? file.getSize() : null;
    }

    /**
     * 获取文件哈希值
     */
    public String getFileHash() {
        return file != null ? file.getFileHash() : null;
    }

    /**
     * 检查此文件记录是否与其他记录共享同一文件
     */
    public boolean isSharedStorage() {
        return file != null && file.isShared();
    }
}
