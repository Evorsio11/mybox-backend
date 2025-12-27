package com.evorsio.mybox.storage.service.impl;

import com.evorsio.mybox.common.error.ErrorCode;
import com.evorsio.mybox.storage.domain.File;
import com.evorsio.mybox.storage.domain.FileStatus;
import com.evorsio.mybox.storage.exception.FileException;
import com.evorsio.mybox.storage.repository.FileRepository;
import com.evorsio.mybox.storage.service.FileService;
import com.evorsio.mybox.storage.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final MinioStorageService minioStorageService;
    private final FileRepository fileRepository;

    @Value("${minio.bucket}")
    private String defaultBucket;

    @Override
    public File uploadFile(UUID ownerId, String originalFileName, long size, String contentType, InputStream inputStream) {
        if (originalFileName == null || inputStream == null) {
            log.error("文件上传参数为空: ownerId={}, fileName={}", ownerId, originalFileName);
            throw new FileException(ErrorCode.VALIDATION_ERROR);
        }
        try {
            String objectName = UUID.randomUUID().toString();
            minioStorageService.upload(defaultBucket, objectName, inputStream, size, contentType);

            File file = new File();
            file.setOwnerId(ownerId);
            file.setOriginalFileName(originalFileName);
            file.setObjectName(objectName);
            file.setBucket(defaultBucket);
            file.setContentType(contentType);
            file.setSize(size);
            file.setStatus(FileStatus.ACTIVE);

            File savedFile = fileRepository.save(file);
            log.info("文件上传成功: fileId={}, ownerId={}, fileName={}", savedFile.getId(), ownerId, originalFileName);
            return savedFile;
        } catch (Exception e) {
            log.error("文件上传发生异常: ownerId={}, fileName={}, error={}", ownerId, originalFileName, e.getMessage(), e);
            throw new FileException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public File getActiveFileById(UUID ownerId, UUID fileId) {
        return fileRepository.findByIdAndOwnerIdAndStatus(fileId, ownerId, FileStatus.ACTIVE)
                .orElseThrow(() -> new FileException(ErrorCode.FILE_NOT_FOUND));
    }

    @Override
    public InputStream downloadFile(UUID ownerId, UUID fileId) {
        File file = getActiveFileById(ownerId, fileId);

        try {
            InputStream inputStream = minioStorageService.download(file.getBucket(), file.getObjectName());
            log.info("文件下载成功: ownerId={}, fileId={}, fileName={}", ownerId, fileId, file.getOriginalFileName());
            return inputStream;
        } catch (Exception e) {
            log.error("文件下载发生异常: ownerId={}, fileId={}, error={}", ownerId, fileId, e.getMessage(), e);
            throw new FileException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void deleteFile(UUID ownerId, UUID fileId) {
        File file = getActiveFileById(ownerId, fileId);

        try {
            minioStorageService.delete(file.getBucket(), file.getObjectName());
            file.setStatus(FileStatus.DELETED);
            fileRepository.save(file);
            log.info("文件删除成功: ownerId={}, fileId={}, fileName={}", ownerId, fileId, file.getOriginalFileName());
        } catch (Exception e) {
            log.error("文件删除发生异常: ownerId={}, fileId={}, error={}", ownerId, fileId, e.getMessage(), e);
            throw new FileException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public List<File> listFiles(UUID ownerId) {
        return fileRepository.findByOwnerIdAndStatus(ownerId, FileStatus.ACTIVE);
    }

    @Override
    public List<File> listDeletedFiles(UUID ownerId) {
        return fileRepository.findByOwnerIdAndStatus(ownerId, FileStatus.DELETED);
    }

    @Override
    public void restoreFile(UUID ownerId, UUID fileId) {
        File file = fileRepository.findByIdAndOwnerIdAndStatus(fileId, ownerId, FileStatus.DELETED)
                .orElseThrow(() -> {
                    log.warn("恢复文件失败, 文件未找到: ownerId={}, fileId={}", ownerId, fileId);
                    return new FileException(ErrorCode.FILE_NOT_FOUND);
                });
        file.setStatus(FileStatus.ACTIVE);
        fileRepository.save(file);
        log.info("文件恢复成功: ownerId={}, fileId={}, fileName={}", ownerId, fileId, file.getOriginalFileName());
    }
}
