package com.evorsio.mybox.file.internal.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.evorsio.mybox.file.FileRecord;
import com.evorsio.mybox.file.FileStatus;

/**
 * 文件记录数据访问层
 * <p>
 * 管理用户视角的文件记录
 */
public interface FileRecordRepository extends JpaRepository<FileRecord, UUID> {
    Optional<FileRecord> findByIdAndOwnerIdAndStatus(UUID id, UUID ownerId, FileStatus status);

    List<FileRecord> findByOwnerIdAndStatus(UUID ownerId, FileStatus status);

    /**
     * 根据文件的哈希值查找文件记录（全局去重）
     */
    @Query("SELECT fr FROM FileRecord fr WHERE fr.file.fileHash = :fileHash AND fr.status = :status")
    List<FileRecord> findByFileHashAndStatus(@Param("fileHash") String fileHash, @Param("status") FileStatus status);

    /**
     * 检查是否存在引用指定文件的文件记录
     */
    @Query("SELECT COUNT(fr) > 0 FROM FileRecord fr WHERE fr.fileId = :fileId AND fr.status = :status")
    boolean existsByFileIdAndStatus(@Param("fileId") UUID fileId, @Param("status") FileStatus status);

    /**
     * 统计引用指定文件的活跃文件记录数量
     */
    @Query("SELECT COUNT(fr) FROM FileRecord fr WHERE fr.fileId = :fileId AND fr.status = :status")
    long countByFileIdAndStatus(@Param("fileId") UUID fileId, @Param("status") FileStatus status);

    /**
     * 统计用户文件记录总大小（通过 File）
     */
    @Query("SELECT COALESCE(SUM(f.size), 0) FROM FileRecord fr JOIN fr.file f WHERE fr.ownerId = :ownerId AND fr.status = :status")
    long sumActiveFileRecordSizeByOwnerId(@Param("ownerId") UUID ownerId, @Param("status") FileStatus status);

    /**
     * 统计用户实际存储使用量（基于 File 去重）
     */
    @Query("SELECT COALESCE(SUM(DISTINCT f.size), 0) FROM FileRecord fr JOIN fr.file f WHERE fr.ownerId = :ownerId AND fr.status = :status")
    long sumActualStorageSizeByOwnerId(@Param("ownerId") UUID ownerId, @Param("status") FileStatus status);

    List<FileRecord> findByStatusAndDeletedAtBefore(FileStatus fileStatus, LocalDateTime expireTime);

    /**
     * 查询指定文件夹内的文件记录
     */
    List<FileRecord> findByFolderIdAndOwnerIdAndStatus(UUID folderId, UUID ownerId, FileStatus status);

    /**
     * 查询未分类文件记录（folderId 为 null）
     */
    List<FileRecord> findByFolderIdIsNullAndOwnerIdAndStatus(UUID ownerId, FileStatus status);

    boolean existsByOwnerIdAndFolderIdAndOriginalFileNameAndStatus(UUID ownerId, UUID folderId, String originalFileName, FileStatus status);

    boolean existsByOwnerIdAndFolderIdIsNullAndOriginalFileNameAndStatus(UUID ownerId, String originalFileName, FileStatus status);
}
