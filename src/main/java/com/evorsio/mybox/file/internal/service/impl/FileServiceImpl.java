package com.evorsio.mybox.file.internal.service.impl;

import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.file.File;
import com.evorsio.mybox.file.FileStatus;
import com.evorsio.mybox.file.internal.exception.FileException;
import com.evorsio.mybox.file.internal.repository.FileRepository;
import com.evorsio.mybox.file.FileConfigService;
import com.evorsio.mybox.file.FileService;
import com.evorsio.mybox.file.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final MinioStorageService minioStorageService;
    private final FileRepository fileRepository;
    private final FileConfigService fileConfigService;

    @Value("${minio.bucket}")
    private String defaultBucket;

    @Override
    public File uploadFile(UUID ownerId, String originalFileName, long size, String contentType, InputStream inputStream) {
        if (originalFileName == null || inputStream == null) {
            log.error("文件上传参数为空: ownerId={}, fileName={}", ownerId, originalFileName);
            throw new FileException(ErrorCode.VALIDATION_ERROR);
        }

        // 0. 单文件大小校验（必须）
        if (size > fileConfigService.getMaxFileSize()) {
            log.warn("文件过大: ownerId={}, size={}, limit={}",
                    ownerId, size, fileConfigService.getMaxFileSize());
            throw new FileException(ErrorCode.FILE_TOO_LARGE);
        }

        long usedSize = fileRepository.sumActiveFileSizeByOwnerId(ownerId, FileStatus.ACTIVE);

        if (usedSize + size > fileConfigService.getMaxTotalStorageSize()) {
            log.warn("存储空间不足: ownerId={}, used={}, upload={}, limit={}",
                    ownerId, usedSize, size, fileConfigService.getMaxTotalStorageSize());
            throw new FileException(ErrorCode.STORAGE_FULL);
        }

        // 1. 校验文件后缀
        String ext = FilenameUtils.getExtension(originalFileName).toLowerCase();
        if (fileConfigService.isExtensionRejected(ext)) {
            log.warn("文件扩展名不允许: {}, allowAllFileTypes={}", ext,
                    fileConfigService.isAllowAllFileTypes());
            throw new FileException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        // 2. 校验 MIME 类型
        if (fileConfigService.isContentTypeRejected(contentType)) {
            log.warn("文件 MIME 类型不允许: {}, allowAllFileTypes={}", contentType,
                    fileConfigService.isAllowAllFileTypes());
            throw new FileException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        String objectName = UUID.randomUUID().toString();
        String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8);
        try {
            // 3. 使用 DigestInputStream 边上传边计算 SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);

            // 上传到 MinIO
            minioStorageService.upload(defaultBucket, objectName, digestInputStream, size, contentType);

            // 上传完成后获取 Hash
            String fileHash = Hex.encodeHexString(digest.digest());

            // 4. 检查数据库是否已有相同文件
            File existingFile = fileRepository.findByOwnerIdAndFileHashAndStatus(ownerId, fileHash, FileStatus.ACTIVE);
            if (existingFile != null) {
                // 文件重复，删除刚上传的 MinIO 对象
                minioStorageService.delete(defaultBucket, objectName);
                return existingFile;
            }

            // 5. 保存数据库记录
            File file = new File();
            file.setOwnerId(ownerId);
            file.setOriginalFileName(encodedFileName);
            file.setObjectName(objectName);
            file.setBucket(defaultBucket);
            file.setContentType(contentType);
            file.setSize(size);
            file.setFileHash(fileHash);
            file.setStatus(FileStatus.ACTIVE);

            File savedFile = fileRepository.save(file);
            log.info("文件上传成功: fileId={}, ownerId={}, fileName={}",
                    savedFile.getId(), ownerId, savedFile.getOriginalFileName());

            return savedFile;

        } catch (Exception e) {
            try {
                minioStorageService.delete(defaultBucket, objectName);
            } catch (Exception ex) {
                log.error("回滚 MinIO 文件失败: objectName={}, error={}", objectName, ex.getMessage(), ex);
            }
            throw new RuntimeException("文件上传失败", e);
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
    public InputStream downloadPartialFile(UUID ownerId, UUID fileId, long start, long end) {
        File file = getActiveFileById(ownerId, fileId);

        try {
            // 计算长度（end 是包含的，所以需要 +1）
            long length = end - start + 1;

            InputStream inputStream = minioStorageService.downloadPartial(
                    file.getBucket(),
                    file.getObjectName(),
                    start,
                    length
            );

            log.info("部分文件下载成功: ownerId={}, fileId={}, range={}-{}",
                    ownerId, fileId, start, end);

            return inputStream;
        } catch (Exception e) {
            log.error("部分文件下载发生异常: ownerId={}, fileId={}, range={}-{}, error={}",
                    ownerId, fileId, start, end, e.getMessage(), e);
            throw new FileException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void deleteFile(UUID ownerId, UUID fileId) {
        File file = getActiveFileById(ownerId, fileId);

        try {
            // 删除 MinIO 中的文件
            minioStorageService.delete(file.getBucket(), file.getObjectName());

            // 使用实体的业务方法标记删除（会同时设置 status 和 deletedAt）
            file.markAsDeleted();
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

        // 使用实体的业务方法恢复（会同时设置 status 为 ACTIVE 和清空 deletedAt）
        file.restore();
        fileRepository.save(file);

        log.info("文件恢复成功: ownerId={}, fileId={}, fileName={}", ownerId, fileId, file.getOriginalFileName());
    }
}
