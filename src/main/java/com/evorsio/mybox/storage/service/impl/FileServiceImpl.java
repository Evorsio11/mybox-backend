package com.evorsio.mybox.storage.service.impl;

import com.evorsio.mybox.storage.domain.File;
import com.evorsio.mybox.storage.domain.FileStatus;
import com.evorsio.mybox.storage.repository.FileRepository;
import com.evorsio.mybox.storage.service.FileService;
import com.evorsio.mybox.storage.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final MinioStorageService minioStorageService;
    private final FileRepository fileRepository;

    @Value("${minio.bucket}")
    private String defaultBucket;

    @Override
    public File uploadFile(UUID ownerId, String originalFileName, long size, String contentType, InputStream inputStream) {
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

            return fileRepository.save(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File getActiveFileById(UUID ownerId, UUID fileId) {
        return fileRepository.findByIdAndOwnerIdAndStatus(fileId, ownerId, FileStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("文件不存在或已删除"));
    }

    @Override
    public InputStream downloadFile(UUID ownerId, UUID fileId) {
        File file = fileRepository.findByIdAndOwnerIdAndStatus(fileId, ownerId, FileStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("文件不存在或已经删除"));

        return minioStorageService.download(file.getBucket(), file.getObjectName());
    }

    @Override
    public void deleteFile(UUID ownerId, UUID fileId) {
        File file = fileRepository.findByIdAndOwnerIdAndStatus(fileId, ownerId, FileStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("文件不存在或已经删除"));

        minioStorageService.delete(file.getBucket(), file.getObjectName());

        file.setStatus(FileStatus.DELETED);
        fileRepository.save(file);
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
                .orElseThrow(() -> new RuntimeException("文件不存在或已经删除"));

        file.setStatus(FileStatus.ACTIVE);
        fileRepository.save(file);
    }
}
