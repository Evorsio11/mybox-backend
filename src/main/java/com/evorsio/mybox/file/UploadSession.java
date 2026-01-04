package com.evorsio.mybox.file;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "upload_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    @Builder.Default
    private UUID id = null;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String contentType;

    @Column(length = 64)
    private String fileHash;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private Integer totalChunks;

    @Column()
    @Builder.Default
    private Integer uploadedChunks = 0;

    @Column(nullable = false)
    private Long chunkSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadStatus status;

    /**
     * 文件唯一标识（用于统一接口关联分片）
     */
    @Column(length = 255)
    private String fileIdentifier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column()
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
