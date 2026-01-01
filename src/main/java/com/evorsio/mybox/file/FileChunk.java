package com.evorsio.mybox.file;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    @Builder.Default
    private UUID id = null;

    @Column(nullable = false)
    private UUID uploadSessionId;

    @Column(nullable = false)
    private Integer chunkNumber;

    @Column(nullable = false)
    private Long chunkSize;

    @Column(length = 64)
    private String chunkHash;

    @Column(nullable = false)
    private String objectName;

    @Column(nullable = false)
    private String bucket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChunkStatus status;

    @Column()
    private LocalDateTime uploadedAt;

    @Column()
    @Builder.Default
    private Integer retryCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ChunkStatus.PENDING;
        }
    }
}
