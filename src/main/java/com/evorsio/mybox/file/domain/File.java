package com.evorsio.mybox.file.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "files")
@Comment("文件元数据表，对应 MinIO 中的对象信息")
public class File {
    @Id
    @GeneratedValue
    @Comment("文件记录唯一标识（数据库主键）")
    private UUID id;

    @Column(nullable = false)
    @Comment("用户上传时的原始文件名")
    private String originalFileName;

    @Column(nullable = false, unique = true)
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
}
