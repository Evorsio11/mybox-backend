package com.evorsio.mybox.file.internal.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.evorsio.mybox.file.UploadSession;
import com.evorsio.mybox.file.UploadStatus;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

    List<UploadSession> findByOwnerIdAndStatus(UUID ownerId, UploadStatus status);

    List<UploadSession> findByStatusAndCreatedAtBefore(UploadStatus status, LocalDateTime time);

    Optional<UploadSession> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying
    @Transactional
    @Query("UPDATE UploadSession s SET s.uploadedChunks = :uploadedChunks, s.status = :status WHERE s.id = :id")
    void updateUploadedChunksAndStatus(@Param("id") UUID id,
                                       @Param("uploadedChunks") Integer uploadedChunks,
                                       @Param("status") UploadStatus status);

    /**
     * 查找用户创建的、指定文件名和大小的所有上传会话（用于并发冲突处理）
     * 如果 fileSize 为 null，则只按文件名查询
     */
    @Query("SELECT s FROM UploadSession s WHERE s.ownerId = :ownerId AND s.originalFileName = :fileName AND (:fileSize IS NULL OR s.fileSize = :fileSize) ORDER BY s.createdAt DESC")
    List<UploadSession> findSessionsByFile(@Param("ownerId") UUID ownerId,
                                          @Param("fileName") String fileName,
                                          @Param("fileSize") Long fileSize);

    /**
     * 查找用户最近创建的、指定文件名和大小的上传会话（用于幂等性检查）
     */
    @Query("SELECT s FROM UploadSession s WHERE s.ownerId = :ownerId AND s.originalFileName = :fileName AND s.fileSize = :fileSize AND s.createdAt > :since ORDER BY s.createdAt DESC")
    List<UploadSession> findRecentSessionsByFile(@Param("ownerId") UUID ownerId,
                                                  @Param("fileName") String fileName,
                                                  @Param("fileSize") Long fileSize,
                                                  @Param("since") LocalDateTime since);

    /**
     * 根据文件标识和用户ID查找上传会话（用于统一接口）
     */
    Optional<UploadSession> findByOwnerIdAndFileIdentifier(UUID ownerId, String fileIdentifier);
}
