package com.evorsio.mybox.file.internal.repository;

import com.evorsio.mybox.file.ChunkStatus;
import com.evorsio.mybox.file.FileChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileChunkRepository extends JpaRepository<FileChunk, UUID> {

    List<FileChunk> findByUploadSessionId(UUID uploadSessionId);

    Optional<FileChunk> findByUploadSessionIdAndChunkNumber(UUID uploadSessionId, Integer chunkNumber);

    @Modifying
    @Transactional
    void deleteByUploadSessionId(UUID uploadSessionId);

    @Modifying
    @Transactional
    @Query("UPDATE FileChunk c SET c.status = :status, c.chunkHash = :chunkHash, c.uploadedAt = :uploadedAt WHERE c.id = :id")
    void updateChunkStatus(@Param("id") UUID id,
                          @Param("status") ChunkStatus status,
                          @Param("chunkHash") String chunkHash,
                          @Param("uploadedAt") java.time.LocalDateTime uploadedAt);

    long countByUploadSessionIdAndStatus(UUID uploadSessionId, ChunkStatus status);
}
