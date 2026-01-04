package com.evorsio.mybox.file.internal.service.impl;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.file.File;
import com.evorsio.mybox.file.FileConfigService;
import com.evorsio.mybox.file.FileService;
import com.evorsio.mybox.file.FileStatus;
import com.evorsio.mybox.file.MinioStorageService;
import com.evorsio.mybox.file.internal.exception.FileException;
import com.evorsio.mybox.file.internal.repository.FileRepository;
import com.evorsio.mybox.folder.FolderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final MinioStorageService minioStorageService;
    private final FileRepository fileRepository;
    private final FileConfigService fileConfigService;
    private final FolderService folderService;

    @Value("${minio.bucket}")
    private String defaultBucket;

    @Override
    public File uploadFile(UUID ownerId, String originalFileName, long size, String contentType, InputStream inputStream) {
        return uploadFile(ownerId, null, originalFileName, size, contentType, inputStream);
    }

    @Override
    public File uploadFile(UUID ownerId, UUID folderId, String originalFileName, long size, String contentType, InputStream inputStream) {
        if (originalFileName == null || inputStream == null) {
            log.error("文件上传参数为空: ownerId={}, fileName={}", ownerId, originalFileName);
            throw new FileException(ErrorCode.VALIDATION_ERROR);
        }

        // 验证文件夹是否存在且属于该用户
        if (folderId != null) {
            try {
                folderService.getFolderDetails(folderId, ownerId);
            } catch (Exception e) {
                log.error("文件夹不存在或不属于该用户: folderId={}, ownerId={}", folderId, ownerId);
                throw new FileException(ErrorCode.FOLDER_NOT_FOUND);
            }
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
        String resolvedFileName = resolveFileName(ownerId, folderId, originalFileName);
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
                // 文件重复，删除刚上传的 MinIO 对象，然后创建新的元数据记录复用已有对象
                minioStorageService.delete(defaultBucket, objectName);

                File duplicateFile = File.builder()
                        .ownerId(ownerId)
                        .folderId(folderId)
                        .originalFileName(resolvedFileName)
                        .objectName(existingFile.getObjectName())
                        .bucket(existingFile.getBucket())
                        .contentType(existingFile.getContentType())
                        .size(existingFile.getSize())
                        .fileHash(existingFile.getFileHash())
                        .status(FileStatus.ACTIVE)
                        .build();

                File savedDuplicate = fileRepository.save(duplicateFile);
                log.info("文件重复上传，生成新记录: fileId={}, ownerId={}, fileName={} -> {}",
                        savedDuplicate.getId(), ownerId, originalFileName, savedDuplicate.getOriginalFileName());

                return savedDuplicate;
            }

            // 5. 保存数据库记录
            File file = File.builder()
                    .ownerId(ownerId)
                    .folderId(folderId)
                    .originalFileName(resolvedFileName)
                    .objectName(objectName)
                    .bucket(defaultBucket)
                    .contentType(contentType)
                    .size(size)
                    .fileHash(fileHash)
                    .status(FileStatus.ACTIVE)
                    .build();

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

    private String resolveFileName(UUID ownerId, UUID folderId, String originalFileName) {
        String baseName = FilenameUtils.getBaseName(originalFileName);
        String extension = FilenameUtils.getExtension(originalFileName);
        String candidateName = originalFileName;
        String encodedCandidate = URLEncoder.encode(candidateName, StandardCharsets.UTF_8);
        int counter = 1;

        while (fileNameExists(ownerId, folderId, encodedCandidate)) {
            String suffix = extension.isEmpty() ? "" : "." + extension;
            candidateName = String.format("%s(%d)%s", baseName, counter++, suffix);
            encodedCandidate = URLEncoder.encode(candidateName, StandardCharsets.UTF_8);
        }

        return encodedCandidate;
    }

    private boolean fileNameExists(UUID ownerId, UUID folderId, String encodedFileName) {
        if (folderId == null) {
            return fileRepository.existsByOwnerIdAndFolderIdIsNullAndOriginalFileNameAndStatus(
                    ownerId, encodedFileName, FileStatus.ACTIVE);
        }

        return fileRepository.existsByOwnerIdAndFolderIdAndOriginalFileNameAndStatus(
                ownerId, folderId, encodedFileName, FileStatus.ACTIVE);
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

    @Override
    public List<File> listFilesByFolder(UUID ownerId, UUID folderId) {
        // 验证文件夹是否存在且属于该用户
        if (folderId != null) {
            try {
                folderService.getFolderDetails(folderId, ownerId);
            } catch (Exception e) {
                log.error("文件夹不存在或不属于该用户: folderId={}, ownerId={}", folderId, ownerId);
                throw new FileException(ErrorCode.FOLDER_NOT_FOUND);
            }
        }

        return fileRepository.findByFolderIdAndOwnerIdAndStatus(folderId, ownerId, FileStatus.ACTIVE);
    }

    @Override
    public List<File> listUnclassifiedFiles(UUID ownerId) {
        return fileRepository.findByFolderIdIsNullAndOwnerIdAndStatus(ownerId, FileStatus.ACTIVE);
    }

    @Override
    public File moveFileToFolder(UUID ownerId, UUID fileId, UUID targetFolderId) {
        // 1. 验证文件是否存在且属于该用户
        File file = getActiveFileById(ownerId, fileId);

        // 2. 验证目标文件夹（如果不为 null）
        if (targetFolderId != null) {
            try {
                folderService.getFolderDetails(targetFolderId, ownerId);
            } catch (Exception e) {
                log.error("目标文件夹不存在或不属于该用户: folderId={}, ownerId={}", targetFolderId, ownerId);
                throw new FileException(ErrorCode.FOLDER_NOT_FOUND);
            }
        }

        // 3. 更新文件的 folderId
        file.setFolderId(targetFolderId);
        File savedFile = fileRepository.save(file);

        log.info("文件移动成功: fileId={}, ownerId={}, fromFolder={}, toFolder={}",
                fileId, ownerId, file.getFolderId(), targetFolderId);

        return savedFile;
    }
}
