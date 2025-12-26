package com.evorsio.mybox.storage.repository;

import com.evorsio.mybox.storage.domain.File;
import com.evorsio.mybox.storage.domain.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {
    Optional<File> findByIdAndOwnerIdAndStatus(UUID id, UUID ownerId, FileStatus status);

    List<File> findByOwnerIdAndStatus(UUID ownerId, FileStatus status);
}
