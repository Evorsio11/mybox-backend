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
 * 文件向量实体 - 用于AI语义搜索
 * <p>
 * 存储文件的AI向量嵌入，支持语义相似度搜索
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "file_vectors", indexes = {
        @Index(name = "idx_file_vector_file_id", columnList = "fileId", unique = true),
        @Index(name = "idx_file_vector_owner", columnList = "ownerId"),
        @Index(name = "idx_file_vector_status", columnList = "status"),
        @Index(name = "idx_file_vector_model", columnList = "modelVersion")
})
@Comment("文件向量表，用于AI语义搜索和相似度计算")
public class FileVector {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Comment("向量记录唯一标识")
    private UUID id;

    @Column(name = "file_id", nullable = false, unique = true)
    @Comment("关联的文件记录ID（唯一）")
    private UUID fileId;

    @Column(name = "owner_id", nullable = false)
    @Comment("文件所有者ID")
    private UUID ownerId;

    /**
     * 向量嵌入数据
     * 使用 PostgreSQL 的 pgvector 扩展存储
     * dimension: 1024 (根据使用的嵌入模型而定)
     * 注意：pgvector 在 JPA 中使用 String 存储，格式为 "[0.1,0.2,...]"
     */
    @Column(nullable = false, columnDefinition = "vector(1024)")
    @Comment("AI向量嵌入（1024维向量，用于语义搜索）")
    private String embedding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("向量索引状态")
    @Builder.Default
    private IndexStatus status = IndexStatus.PENDING;

    @Column(nullable = false, length = 100)
    @Comment("嵌入模型版本（如 text-embedding-3-large, text-embedding-3-small）")
    private String modelVersion;

    @Column(nullable = false)
    @Comment("向量维度（必须与模型一致）")
    private Integer dimension;

    @Column
    @Comment("嵌入生成时使用的文本内容摘要（用于验证）")
    private String contentHash;

    @Column(nullable = false, updatable = false)
    @Comment("向量创建时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("向量更新时间")
    private LocalDateTime updatedAt;

    @Column
    @Comment("文件最后修改时间（用于检测文件更新，触发重新嵌入）")
    private LocalDateTime fileModifiedAt;

    @Column
    @Comment("嵌入错误信息（生成失败时记录）")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = IndexStatus.PENDING;
        }
        // 默认使用 OpenAI text-embedding-3-large 模型（1024维）
        if (modelVersion == null) {
            modelVersion = "text-embedding-3-large";
        }
        if (dimension == null) {
            dimension = 1024;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否需要重新生成向量
     */
    public boolean needsRegeneration(LocalDateTime currentFileModifiedAt) {
        return status == IndexStatus.PENDING
                || status == IndexStatus.FAILED
                || status == IndexStatus.OUTDATED
                || (currentFileModifiedAt != null && !currentFileModifiedAt.equals(fileModifiedAt));
    }

    /**
     * 标记为生成中
     */
    public void markAsIndexing() {
        this.status = IndexStatus.INDEXING;
        this.errorMessage = null;
    }

    /**
     * 标记为已生成
     */
    public void markAsIndexed() {
        this.status = IndexStatus.INDEXED;
        this.errorMessage = null;
    }

    /**
     * 标记为生成失败
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

    /**
     * 计算与另一个向量的余弦相似度
     * 注意：实际计算通常在数据库层面使用 pgvector 的操作符完成
     * 这个方法主要用于调试和测试
     */
    public double cosineSimilarity(String other) {
        if (embedding == null || other == null) {
            return 0.0;
        }

        // 解析向量字符串 "[0.1,0.2,...]" 为 float 数组
        float[] vec1 = parseVector(embedding);
        float[] vec2 = parseVector(other);

        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 解析向量字符串为 float 数组
     */
    private float[] parseVector(String vectorStr) {
        if (vectorStr == null || vectorStr.isEmpty()) {
            return null;
        }

        try {
            // 移除方括号并分割
            String cleaned = vectorStr.replace("[", "").replace("]", "").trim();
            if (cleaned.isEmpty()) {
                return null;
            }

            String[] parts = cleaned.split(",");
            float[] vector = new float[parts.length];

            for (int i = 0; i < parts.length; i++) {
                vector[i] = Float.parseFloat(parts[i].trim());
            }

            return vector;
        } catch (Exception e) {
            return null;
        }
    }
}
