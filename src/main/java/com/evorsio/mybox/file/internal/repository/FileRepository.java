package com.evorsio.mybox.file.internal.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.evorsio.mybox.file.File;
import com.evorsio.mybox.file.FileStatus;

public interface FileRepository extends JpaRepository<File, UUID> {
    Optional<File> findByIdAndOwnerIdAndStatus(UUID id, UUID ownerId, FileStatus status);

    List<File> findByOwnerIdAndStatus(UUID ownerId, FileStatus status);

    File findByOwnerIdAndFileHashAndStatus(UUID ownerId, String fileHash, FileStatus fileStatus);

    @Query("SELECT COALESCE(SUM(f.size), 0) FROM File f WHERE f.ownerId = :ownerId AND f.status = :status")
    long sumActiveFileSizeByOwnerId(@Param("ownerId") UUID ownerId, @Param("status") FileStatus status);

    List<File> findByStatusAndDeletedAtBefore(FileStatus fileStatus, LocalDateTime expireTime);

    /**
     * 查询指定文件夹内的文件
     */
    List<File> findByFolderIdAndOwnerIdAndStatus(UUID folderId, UUID ownerId, FileStatus status);

    /**
     * 查询未分类文件（folderId 为 null）
     */
    List<File> findByFolderIdIsNullAndOwnerIdAndStatus(UUID ownerId, FileStatus status);

    boolean existsByOwnerIdAndFolderIdAndOriginalFileNameAndStatus(UUID ownerId, UUID folderId, String originalFileName, FileStatus status);

    boolean existsByOwnerIdAndFolderIdIsNullAndOriginalFileNameAndStatus(UUID ownerId, String originalFileName, FileStatus status);
}
