package com.evorsio.mybox.file.internal.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evorsio.mybox.file.File;
import com.evorsio.mybox.file.FileService;
import com.evorsio.mybox.file.MinioStorageService;
import com.evorsio.mybox.file.internal.repository.FileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final MinioStorageService minioStorageService;

    @Override
    @Transactional(readOnly = true)
    public Optional<File> findByFileHash(String fileHash) {
        return fileRepository.findByFileHash(fileHash);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByFileHash(String fileHash) {
        return fileRepository.existsByFileHash(fileHash);
    }

    @Override
    @Transactional
    public File createFile(String fileHash, String objectName, String bucket, String contentType, Long size) {
        File file = File.builder()
                .fileHash(fileHash)
                .objectName(objectName)
                .bucket(bucket)
                .contentType(contentType)
                .size(size)
                .referenceCount(1)
                .build();

        File saved = fileRepository.save(file);
        log.info("创建文件: id={}, hash={}, size={}", saved.getId(), fileHash, size);
        return saved;
    }

    @Override
    @Transactional
    public FileResult getOrCreateFile(String fileHash, String objectName, String bucket, String contentType, Long size) {
        // 先尝试查找已存在的文件
        Optional<File> existing = fileRepository.findByFileHash(fileHash);

        if (existing.isPresent()) {
            File file = existing.get();
            // 增加引用计数
            fileRepository.incrementReferenceCount(file.getId());
            log.info("复用文件: id={}, hash={}, newRefCount={}",
                    file.getId(), fileHash, file.getReferenceCount() + 1);
            return new FileResult(file, false);
        }

        // 创建新的文件
        File newFile = createFile(fileHash, objectName, bucket, contentType, size);
        return new FileResult(newFile, true);
    }

    @Override
    @Transactional
    public void incrementReferenceCount(UUID fileId) {
        int updated = fileRepository.incrementReferenceCount(fileId);
        if (updated == 0) {
            log.warn("增加引用计数失败，文件不存在: id={}", fileId);
        } else {
            log.debug("增加引用计数: fileId={}", fileId);
        }
    }

    @Override
    @Transactional
    public int decrementReferenceCount(UUID fileId) {
        int updated = fileRepository.decrementReferenceCount(fileId);
        if (updated == 0) {
            log.warn("减少引用计数失败，文件不存在或已无引用: id={}", fileId);
            return 0;
        }

        Integer newCount = fileRepository.getReferenceCount(fileId);
        log.debug("减少引用计数: fileId={}, newCount={}", fileId, newCount);
        return newCount != null ? newCount : 0;
    }

    @Override
    @Transactional
    public boolean tryDeleteFile(UUID fileId) {
        Optional<File> optionalFile = fileRepository.findById(fileId);
        if (optionalFile.isEmpty()) {
            log.warn("文件不存在: id={}", fileId);
            return false;
        }

        File file = optionalFile.get();
        if (!file.canBeDeleted()) {
            log.debug("文件仍有引用，无法删除: id={}, refCount={}",
                    fileId, file.getReferenceCount());
            return false;
        }

        // 删除 MinIO 中的对象
        try {
            minioStorageService.delete(file.getBucket(), file.getObjectName());
            log.info("删除 MinIO 对象: bucket={}, objectName={}",
                    file.getBucket(), file.getObjectName());
        } catch (Exception e) {
            log.error("删除 MinIO 对象失败: bucket={}, objectName={}, error={}",
                    file.getBucket(), file.getObjectName(), e.getMessage(), e);
            // 继续删除数据库记录，MinIO 清理可以后续重试
        }

        // 删除数据库记录
        fileRepository.delete(file);
        log.info("删除文件记录: id={}, hash={}", fileId, file.getFileHash());
        return true;
    }

    @Override
    @Transactional
    public int cleanupOrphanedFiles() {
        List<File> orphaned = fileRepository.findOrphanedFiles();

        for (File file : orphaned) {
            try {
                // 删除 MinIO 对象
                minioStorageService.delete(file.getBucket(), file.getObjectName());
                log.info("清理孤立 MinIO 对象: bucket={}, objectName={}",
                        file.getBucket(), file.getObjectName());
            } catch (Exception e) {
                log.error("清理 MinIO 对象失败: {}", file.getObjectName(), e);
            }
        }

        // 删除数据库记录
        int deleted = fileRepository.deleteOrphanedFiles();
        log.info("清理孤立文件完成: 删除 {} 条记录", deleted);
        return deleted;
    }

    @Override
    @Transactional(readOnly = true)
    public long getActualStorageSize() {
        return fileRepository.sumActualStorageSize();
    }
}
