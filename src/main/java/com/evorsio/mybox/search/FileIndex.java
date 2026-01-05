package com.evorsio.mybox.search;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文档索引实体 - 用于全文搜索
 * <p>
 * 存储文件的可搜索文本内容，支持元数据模糊搜索
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document_indices", indexes = {
        @Index(name = "idx_doc_index_file_id", columnList = "fileId"),
        @Index(name = "idx_doc_index_owner", columnList = "ownerId"),
        @Index(name = "idx_doc_index_status", columnList = "status"),
        @Index(name = "idx_doc_index_updated", columnList = "updatedAt")
})
@Comment("文档索引表，用于全文搜索和元数据检索")
public class FileIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Comment("索引记录唯一标识")
    private UUID id;

    @Column(name = "file_id", nullable = false)
    @Comment("关联的文件记录ID")
    private UUID fileId;

    @Column(name = "owner_id", nullable = false)
    @Comment("文件所有者ID")
    private UUID ownerId;

    @Column(nullable = false, length = 500)
    @Comment("文件名（用于搜索）")
    private String fileName;

    @Column(columnDefinition = "TEXT")
    @Comment("文件内容文本（从文件中提取的可搜索文本）")
    private String content;

    @Column(length = 1000)
    @Comment("文件描述（用户可编辑的描述信息）")
    private String description;

    @Column(columnDefinition = "TEXT")
    @Comment("文件标签（逗号分隔的标签文本）")
    private String tags;

    @Column(columnDefinition = "TEXT")
    @Comment("扩展元数据（JSON格式的可搜索元数据）")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("索引状态")
    @Builder.Default
    private IndexStatus status = IndexStatus.PENDING;

    @Column(nullable = false)
    @Comment("文件内容类型（MIME类型）")
    private String contentType;

    @Column(nullable = false)
    @Comment("文件大小（字节）")
    private Long fileSize;

    @Column(columnDefinition = "TSVECTOR")
    @Comment("全文搜索向量（PostgreSQL tsvector类型，用于高性能全文搜索）")
    private String textSearchVector;

    @Column(nullable = false, updatable = false)
    @Comment("索引创建时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("索引更新时间")
    private LocalDateTime updatedAt;

    @Column
    @Comment("文件最后修改时间（用于检测文件更新，触发重新索引）")
    private LocalDateTime fileModifiedAt;

    @Column
    @Comment("索引错误信息（索引失败时记录）")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = IndexStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否需要重新索引
     */
    public boolean needsReindex(LocalDateTime currentFileModifiedAt) {
        return status == IndexStatus.PENDING
                || status == IndexStatus.FAILED
                || status == IndexStatus.OUTDATED
                || (currentFileModifiedAt != null && !currentFileModifiedAt.equals(fileModifiedAt));
    }

    /**
     * 标记为索引中
     */
    public void markAsIndexing() {
        this.status = IndexStatus.INDEXING;
        this.errorMessage = null;
    }

    /**
     * 标记为已索引
     */
    public void markAsIndexed() {
        this.status = IndexStatus.INDEXED;
        this.errorMessage = null;
    }

    /**
     * 标记为索引失败
     */
    public void markAsFailed(String error) {
        this.status = IndexStatus.FAILED;
        this.errorMessage = error;
    }

    /**
     * 标记为过期
     */
    public void markAsOutdated() {
        this.status = IndexStatus.OUTDATED;
    }
}
