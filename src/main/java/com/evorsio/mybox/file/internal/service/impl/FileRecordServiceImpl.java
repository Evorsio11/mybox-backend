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
import org.springframework.transaction.annotation.Transactional;

import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.file.File;
import com.evorsio.mybox.file.FileConfigService;
import com.evorsio.mybox.file.FileRecord;
import com.evorsio.mybox.file.FileRecordService;
import com.evorsio.mybox.file.FileService;
import com.evorsio.mybox.file.FileService.FileResult;
import com.evorsio.mybox.file.FileStatus;
import com.evorsio.mybox.file.MinioStorageService;
import com.evorsio.mybox.file.internal.exception.FileException;
import com.evorsio.mybox.file.internal.repository.FileRecordRepository;
import com.evorsio.mybox.folder.FolderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecordServiceImpl implements FileRecordService {
    private final MinioStorageService minioStorageService;
    private final FileRecordRepository fileRecordRepository;
    private final FileConfigService fileConfigService;
    private final FolderService folderService;
    private final FileService fileService;

    @Value("${minio.bucket}")
    private String defaultBucket;

    @Override
    public FileRecord uploadFileRecord(UUID ownerId, String originalFileName, long size, String contentType, InputStream inputStream) {
        return uploadFileRecord(ownerId, null, originalFileName, size, contentType, inputStream);
    }

    @Override
    @Transactional
    public FileRecord uploadFileRecord(UUID ownerId, UUID folderId, String originalFileName, long size, String contentType, InputStream inputStream) {
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

        long usedSize = fileRecordRepository.sumActiveFileRecordSizeByOwnerId(ownerId, FileStatus.ACTIVE);

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

            // 4. 使用 FileService 获取或创建文件（支持全局去重）
            FileResult fileResult = fileService.getOrCreateFile(
                    fileHash, objectName, defaultBucket, contentType, size);

            File file = fileResult.file();

            if (!fileResult.isNew()) {
                // 文件重复，删除刚上传的 MinIO 对象，复用已有文件
                minioStorageService.delete(defaultBucket, objectName);
                log.info("检测到重复文件，复用已有文件: hash={}, fileId={}",
                        fileHash, file.getId());
            }

            // 5. 创建文件记录（关联文件）
            FileRecord fileRecord = FileRecord.builder()
                    .ownerId(ownerId)
                    .folderId(folderId)
                    .originalFileName(resolvedFileName)
                    .file(file)
                    .status(FileStatus.ACTIVE)
                    .build();

            FileRecord savedFileRecord = fileRecordRepository.save(fileRecord);
            log.info("文件记录上传成功: fileRecordId={}, ownerId={}, fileName={}, isNewFile={}",
                    savedFileRecord.getId(), ownerId, savedFileRecord.getOriginalFileName(), fileResult.isNew());

            return savedFileRecord;

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
            return fileRecordRepository.existsByOwnerIdAndFolderIdIsNullAndOriginalFileNameAndStatus(
                    ownerId, encodedFileName, FileStatus.ACTIVE);
        }

        return fileRecordRepository.existsByOwnerIdAndFolderIdAndOriginalFileNameAndStatus(
                ownerId, folderId, encodedFileName, FileStatus.ACTIVE);
    }

    @Override
    public FileRecord getActiveFileRecordById(UUID ownerId, UUID fileRecordId) {
        return fileRecordRepository.findByIdAndOwnerIdAndStatus(fileRecordId, ownerId, FileStatus.ACTIVE)
                .orElseThrow(() -> new FileException(ErrorCode.FILE_NOT_FOUND));
    }

    @Override
    public InputStream downloadFileRecord(UUID ownerId, UUID fileRecordId) {
        FileRecord fileRecord = getActiveFileRecordById(ownerId, fileRecordId);

        try {
            InputStream inputStream = minioStorageService.download(
                    fileRecord.getBucket(), fileRecord.getObjectName());
            log.info("文件记录下载成功: ownerId={}, fileRecordId={}, fileName={}", ownerId, fileRecordId, fileRecord.getOriginalFileName());
            return inputStream;
        } catch (Exception e) {
            log.error("文件记录下载发生异常: ownerId={}, fileRecordId={}, error={}", ownerId, fileRecordId, e.getMessage(), e);
            throw new FileException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public InputStream downloadPartialFileRecord(UUID ownerId, UUID fileRecordId, long start, long end) {
        FileRecord fileRecord = getActiveFileRecordById(ownerId, fileRecordId);

        try {
            // 计算长度（end 是包含的，所以需要 +1）
            long length = end - start + 1;

            InputStream inputStream = minioStorageService.downloadPartial(
                    fileRecord.getBucket(),
                    fileRecord.getObjectName(),
                    start,
                    length
            );

            log.info("部分文件记录下载成功: ownerId={}, fileRecordId={}, range={}-{}",
                    ownerId, fileRecordId, start, end);

            return inputStream;
        } catch (Exception e) {
            log.error("部分文件记录下载发生异常: ownerId={}, fileRecordId={}, range={}-{}, error={}",
                    ownerId, fileRecordId, start, end, e.getMessage(), e);
            throw new FileException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    @Transactional
    public void deleteFileRecord(UUID ownerId, UUID fileRecordId) {
        FileRecord fileRecord = getActiveFileRecordById(ownerId, fileRecordId);

        try {
            // 使用实体的业务方法标记删除（会同时设置 status 和 deletedAt）
            fileRecord.markAsDeleted();
            fileRecordRepository.save(fileRecord);

            // 减少文件的引用计数
            int newRefCount = fileService.decrementReferenceCount(fileRecord.getFileId());
            log.info("文件引用计数减少: fileId={}, newRefCount={}",
                    fileRecord.getFileId(), newRefCount);

            // 如果引用计数降为0，可以考虑延迟删除文件（这里先不删除，由定时任务处理）
            if (newRefCount == 0) {
                log.info("文件无引用，可被清理: fileId={}", fileRecord.getFileId());
            }

            log.info("文件记录删除成功: ownerId={}, fileRecordId={}, fileName={}", ownerId, fileRecordId, fileRecord.getOriginalFileName());
        } catch (Exception e) {
            log.error("文件记录删除发生异常: ownerId={}, fileRecordId={}, error={}", ownerId, fileRecordId, e.getMessage(), e);
            throw new FileException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public List<FileRecord> listFileRecords(UUID ownerId) {
        return fileRecordRepository.findByOwnerIdAndStatus(ownerId, FileStatus.ACTIVE);
    }

    @Override
    public List<FileRecord> listDeletedFileRecords(UUID ownerId) {
        return fileRecordRepository.findByOwnerIdAndStatus(ownerId, FileStatus.DELETED);
    }

    @Override
    public void restoreFileRecord(UUID ownerId, UUID fileRecordId) {
        FileRecord fileRecord = fileRecordRepository.findByIdAndOwnerIdAndStatus(fileRecordId, ownerId, FileStatus.DELETED)
                .orElseThrow(() -> {
                    log.warn("恢复文件记录失败, 文件记录未找到: ownerId={}, fileRecordId={}", ownerId, fileRecordId);
                    return new FileException(ErrorCode.FILE_NOT_FOUND);
                });

        // 使用实体的业务方法恢复（会同时设置 status 为 ACTIVE 和清空 deletedAt）
        fileRecord.restore();
        fileRecordRepository.save(fileRecord);

        log.info("文件记录恢复成功: ownerId={}, fileRecordId={}, fileName={}", ownerId, fileRecordId, fileRecord.getOriginalFileName());
    }

    @Override
    public List<FileRecord> listFileRecordsByFolder(UUID ownerId, UUID folderId) {
        // 验证文件夹是否存在且属于该用户
        if (folderId != null) {
            try {
                folderService.getFolderDetails(folderId, ownerId);
            } catch (Exception e) {
                log.error("文件夹不存在或不属于该用户: folderId={}, ownerId={}", folderId, ownerId);
                throw new FileException(ErrorCode.FOLDER_NOT_FOUND);
            }
        }

        return fileRecordRepository.findByFolderIdAndOwnerIdAndStatus(folderId, ownerId, FileStatus.ACTIVE);
    }

    @Override
    public List<FileRecord> listUnclassifiedFileRecords(UUID ownerId) {
        return fileRecordRepository.findByFolderIdIsNullAndOwnerIdAndStatus(ownerId, FileStatus.ACTIVE);
    }

    @Override
    public FileRecord moveFileRecordToFolder(UUID ownerId, UUID fileRecordId, UUID targetFolderId) {
        // 1. 验证文件记录是否存在且属于该用户
        FileRecord fileRecord = getActiveFileRecordById(ownerId, fileRecordId);

        // 2. 验证目标文件夹（如果不为 null）
        if (targetFolderId != null) {
            try {
                folderService.getFolderDetails(targetFolderId, ownerId);
            } catch (Exception e) {
                log.error("目标文件夹不存在或不属于该用户: folderId={}, ownerId={}", targetFolderId, ownerId);
                throw new FileException(ErrorCode.FOLDER_NOT_FOUND);
            }
        }

        // 3. 更新文件记录的 folderId
        fileRecord.setFolderId(targetFolderId);
        FileRecord savedFileRecord = fileRecordRepository.save(fileRecord);

        log.info("文件记录移动成功: fileRecordId={}, ownerId={}, fromFolder={}, toFolder={}",
                fileRecordId, ownerId, fileRecord.getFolderId(), targetFolderId);

        return savedFileRecord;
    }
}
