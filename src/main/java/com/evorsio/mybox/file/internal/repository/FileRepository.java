package com.evorsio.mybox.file.internal.repository;

import com.evorsio.mybox.file.File;
import com.evorsio.mybox.file.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {
    Optional<File> findByIdAndOwnerIdAndStatus(UUID id, UUID ownerId, FileStatus status);

    List<File> findByOwnerIdAndStatus(UUID ownerId, FileStatus status);

    File findByOwnerIdAndFileHashAndStatus(UUID ownerId, String fileHash, FileStatus fileStatus);

    @Query("SELECT COALESCE(SUM(f.size), 0) FROM File f WHERE f.ownerId = :ownerId AND f.status = :status")
    long sumActiveFileSizeByOwnerId(@Param("ownerId") UUID ownerId, @Param("status") FileStatus status);

    List<File> findByStatusAndDeletedAtBefore(FileStatus fileStatus, LocalDateTime expireTime);
}
