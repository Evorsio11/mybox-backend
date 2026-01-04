package com.evorsio.mybox.file.internal.service.impl;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.evorsio.mybox.common.ErrorCode;
import com.evorsio.mybox.file.ChunkStatus;
import com.evorsio.mybox.file.ChunkUploadService;
import com.evorsio.mybox.file.File;
import com.evorsio.mybox.file.FileChunk;
import com.evorsio.mybox.file.FileConfigService;
import com.evorsio.mybox.file.FileStatus;
import com.evorsio.mybox.file.MinioStorageService;
import com.evorsio.mybox.file.UnifiedChunkUploadRequest;
import com.evorsio.mybox.file.UnifiedChunkUploadResponse;
import com.evorsio.mybox.file.UploadSession;
import com.evorsio.mybox.file.UploadStatus;
import com.evorsio.mybox.file.internal.exception.FileException;
import com.evorsio.mybox.file.internal.repository.FileChunkRepository;
import com.evorsio.mybox.file.internal.repository.FileRepository;
import com.evorsio.mybox.file.internal.repository.UploadSessionRepository;
import com.evorsio.mybox.file.service.ChunkUploadConcurrencyManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkUploadServiceImpl implements ChunkUploadService {
    private final UploadSessionRepository uploadSessionRepository;
    private final FileChunkRepository fileChunkRepository;
    private final FileRepository fileRepository;
    private final MinioStorageService minioStorageService;
    private final ChunkUploadConcurrencyManager concurrencyManager;
    private final FileConfigService fileConfigService;

    @Value("${minio.bucket}")
    private String defaultBucket;

    @Override
    @Transactional
    public UnifiedChunkUploadResponse uploadChunkUnified(UUID ownerId, UnifiedChunkUploadRequest request) {
        long methodStartTime = System.currentTimeMillis();

        // 1. 检查分片上传功能是否启用
        long step1Start = System.currentTimeMillis();
        if (!fileConfigService.isChunkUploadEnabled()) {
            throw new FileException(ErrorCode.CHUNK_UPLOAD_DISABLED);
        }
        log.debug("[步骤1] 检查配置耗时: {}ms", System.currentTimeMillis() - step1Start);

        // 2. 查找或创建上传会话
        long step2Start = System.currentTimeMillis();
        UploadSession session = uploadSessionRepository
                .findByOwnerIdAndFileIdentifier(ownerId, request.getFileIdentifier())
                .orElseGet(() -> {
                    log.info("[数据库查询] 会话不存在，需要创建新会话: fileIdentifier={}", request.getFileIdentifier());
                    return createSessionForUnifiedUpload(ownerId, request);
                });
        log.info("[数据库查询] 查询/创建会话耗时: {}ms, sessionId={}", System.currentTimeMillis() - step2Start, session.getId());

        // 3. 校验分片编号
        long step3Start = System.currentTimeMillis();
        if (request.getChunkNumber() < 1 || request.getChunkNumber() > session.getTotalChunks()) {
            throw new FileException(ErrorCode.CHUNK_NUMBER_INVALID);
        }
        log.debug("[步骤3] 校验分片编号耗时: {}ms", System.currentTimeMillis() - step3Start);

        // 4. 查询或创建分片记录
        long step4Start = System.currentTimeMillis();
        FileChunk chunk = fileChunkRepository.findByUploadSessionIdAndChunkNumber(
                        session.getId(), request.getChunkNumber())
                .orElseGet(() -> {
                    log.info("[数据库查询] 分片记录不存在，自动创建: uploadId={}, chunkNumber={}",
                            session.getId(), request.getChunkNumber());

                    long createStart = System.currentTimeMillis();
                    FileChunk newChunk = FileChunk.builder()
                            .uploadSessionId(session.getId())
                            .chunkNumber(request.getChunkNumber())
                            .chunkSize(session.getChunkSize())
                            .bucket(session.getBucket())
                            .objectName(generateChunkObjectName(session.getId(), request.getChunkNumber()))
                            .status(ChunkStatus.PENDING)
                            .build();

                    FileChunk savedChunk = fileChunkRepository.save(newChunk);
                    log.info("[数据库插入] 创建分片记录耗时: {}ms, chunkId={}",
                            System.currentTimeMillis() - createStart, savedChunk.getId());
                    return savedChunk;
                });
        log.info("[数据库查询] 查询分片记录耗时: {}ms, chunkId={}", System.currentTimeMillis() - step4Start, chunk.getId());

        // 5. 如果分片已上传，直接返回进度
        long step5Start = System.currentTimeMillis();
        if (chunk.getStatus() == ChunkStatus.COMPLETED) {
            log.info("分片已上传，跳过: uploadId={}, chunkNumber={}, 耗时={}ms",
                    session.getId(), request.getChunkNumber(), System.currentTimeMillis() - methodStartTime);
            return buildProgressResponse(session, false);
        }
        log.debug("[步骤5] 检查分片状态耗时: {}ms", System.currentTimeMillis() - step5Start);

        // 6. 并发控制
        long step6Start = System.currentTimeMillis();
        if (!concurrencyManager.tryAcquire(session.getId())) {
            throw new FileException(ErrorCode.CONCURRENT_UPLOAD_LIMIT_EXCEEDED);
        }
        log.debug("[步骤6] 并发控制耗时: {}ms", System.currentTimeMillis() - step6Start);

        try {
            // 7. 上传分片
            long step7Start = System.currentTimeMillis();
            String chunkHash = uploadChunkWithRetry(request.getFile(), chunk);
            log.debug("[步骤7] 上传分片到MinIO耗时: {}ms", System.currentTimeMillis() - step7Start);

            // 8. 更新分片状态
            long step8Start = System.currentTimeMillis();
            fileChunkRepository.updateChunkStatus(
                    chunk.getId(),
                    ChunkStatus.COMPLETED,
                    chunkHash,
                    LocalDateTime.now()
            );
            log.info("[数据库更新] 更新分片状态耗时: {}ms, chunkId={}", System.currentTimeMillis() - step8Start, chunk.getId());

            // 9. 更新会话状态和已上传分片数
            long step9Start = System.currentTimeMillis();
            long countStart = System.currentTimeMillis();
            long uploadedCount = fileChunkRepository.countByUploadSessionIdAndStatus(
                    session.getId(), ChunkStatus.COMPLETED);
            log.debug("[数据库查询] 统计已上传分片数耗时: {}ms, count={}", System.currentTimeMillis() - countStart, uploadedCount);

            long updateSessionStart = System.currentTimeMillis();
            uploadSessionRepository.updateUploadedChunksAndStatus(
                    session.getId(),
                    (int) uploadedCount,
                    UploadStatus.UPLOADING
            );
            log.info("[数据库更新] 更新会话状态耗时: {}ms", System.currentTimeMillis() - updateSessionStart);
            log.info("[步骤9] 更新会话状态总耗时: {}ms", System.currentTimeMillis() - step9Start);

            log.info("分片上传成功: uploadId={}, chunkNumber={}, uploaded={}/{}, 总耗时={}ms",
                    session.getId(), request.getChunkNumber(), uploadedCount, session.getTotalChunks(),
                    System.currentTimeMillis() - methodStartTime);

            // 10. 检查是否所有分片都已上传
            if (uploadedCount >= session.getTotalChunks()) {
                log.info("所有分片上传完成，开始合并: uploadId={}", session.getId());
                return mergeChunksForUnifiedUpload(ownerId, session);
            }

            // 11. 返回进度响应
            return buildProgressResponse(session, false);

        } finally {
            concurrencyManager.release(session.getId());
        }
    }

    /**
     * 为统一接口创建上传会话
     */
    @Transactional
    protected UploadSession createSessionForUnifiedUpload(UUID ownerId, UnifiedChunkUploadRequest request) {
        // 1. 校验文件大小
        if (request.getFileSize() > fileConfigService.getMaxFileSize()) {
            throw new FileException(ErrorCode.FILE_TOO_LARGE);
        }

        if (request.getFileSize() < fileConfigService.getChunkUploadMinSize()) {
            throw new FileException(ErrorCode.FILE_TOO_LARGE_FOR_CHUNK);
        }

        // 2. 校验存储空间
        long usedSize = fileRepository.sumActiveFileSizeByOwnerId(ownerId, FileStatus.ACTIVE);
        if (usedSize + request.getFileSize() > fileConfigService.getMaxTotalStorageSize()) {
            throw new FileException(ErrorCode.STORAGE_FULL);
        }

        // 3. 校验文件类型
        String ext = FilenameUtils.getExtension(request.getOriginalFileName()).toLowerCase();
        if (fileConfigService.isExtensionRejected(ext)) {
            log.warn("文件扩展名不允许: {}", ext);
            throw new FileException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        if (fileConfigService.isContentTypeRejected(request.getContentType())) {
            log.warn("文件 MIME 类型不允许: {}", request.getContentType());
            throw new FileException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        // 4. 检查文件名冲突并解决
        String resolvedFileName = resolveUploadFileName(ownerId, request.getOriginalFileName());
        if (!resolvedFileName.equals(request.getOriginalFileName())) {
            log.info("文件名冲突，已自动重命名: {} -> {}", request.getOriginalFileName(), resolvedFileName);
        }

        // 5. 计算分片信息
        long chunkSize = fileConfigService.getChunkUploadChunkSize();

        try {
            UploadSession session = UploadSession.builder()
                    .ownerId(ownerId)
                    .originalFileName(resolvedFileName)  // 使用解决冲突后的文件名
                    .fileSize(request.getFileSize())
                    .contentType(request.getContentType())
                    .totalChunks(request.getTotalChunks())
                    .uploadedChunks(0)
                    .chunkSize(chunkSize)
                    .status(UploadStatus.INIT)
                    .bucket(defaultBucket)
                    .fileIdentifier(request.getFileIdentifier())
                    .expiresAt(LocalDateTime.now().plusHours(fileConfigService.getChunkUploadSessionTimeoutHours()))
                    .build();

            uploadSessionRepository.save(session);

            log.info("创建统一上传会话: uploadId={}, fileIdentifier={}, fileName={}, totalChunks={}",
                    session.getId(), request.getFileIdentifier(), resolvedFileName,
                    request.getTotalChunks());

            return session;

        } catch (Exception e) {
            log.error("创建上传会话失败: fileIdentifier={}", request.getFileIdentifier(), e);
            throw new FileException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 解析文件名，如果已存在则添加 (1), (2) 等后缀
     * 检查 upload_sessions 表中正在上传的文件和 files 表中已完成的文件
     */
    private String resolveUploadFileName(UUID ownerId, String originalFileName) {
        String baseName = FilenameUtils.getBaseName(originalFileName);
        String extension = FilenameUtils.getExtension(originalFileName);
        String candidateName = originalFileName;
        int counter = 1;

        while (uploadFileNameExists(ownerId, candidateName)) {
            String suffix = extension.isEmpty() ? "" : "." + extension;
            candidateName = String.format("%s(%d)%s", baseName, counter++, suffix);
        }

        return candidateName;
    }

    /**
     * 检查文件名是否已存在（包括正在上传的session和已完成的文件）
     */
    private boolean uploadFileNameExists(UUID ownerId, String fileName) {
        // 检查是否有正在上传或已完成但未过期的上传会话使用相同文件名
        boolean sessionExists = !uploadSessionRepository
                .findSessionsByFile(ownerId, fileName, null)
                .stream()
                .filter(s -> s.getStatus() == UploadStatus.INIT || 
                            s.getStatus() == UploadStatus.UPLOADING ||
                            (s.getStatus() == UploadStatus.COMPLETED && s.getExpiresAt().isAfter(LocalDateTime.now())))
                .toList()
                .isEmpty();

        // 检查是否有活跃的文件记录使用相同文件名
        boolean fileExists = fileRepository.existsByOwnerIdAndFolderIdIsNullAndOriginalFileNameAndStatus(
                ownerId, fileName, FileStatus.ACTIVE);

        return sessionExists || fileExists;
    }

    /**
     * 上传分片（带重试）
     */
    private String uploadChunkWithRetry(MultipartFile file, FileChunk chunk) {
        int maxRetryCount = fileConfigService.getChunkUploadMaxRetryCount();
        int retryDelay = fileConfigService.getChunkUploadRetryDelaySeconds();
        String chunkHash = null;

        log.info("[MinIO上传-开始] chunkNumber={}, bucket={}, objectName={}, size={}",
                chunk.getChunkNumber(), chunk.getBucket(), chunk.getObjectName(), file.getSize());

        for (int attempt = 0; attempt <= maxRetryCount; attempt++) {
            long uploadStart = System.currentTimeMillis();
            try {
                InputStream inputStream = file.getInputStream();
                long getStreamTime = System.currentTimeMillis() - uploadStart;
                log.debug("[MinIO上传] 获取InputStream耗时: {}ms", getStreamTime);

                long minioUploadStart = System.currentTimeMillis();

                // 如果启用了分片去重，计算 hash
                if (fileConfigService.isChunkUploadDeduplicationEnabled() &&
                        "chunk".equals(fileConfigService.getChunkUploadDeduplicationStrategy())) {
                    long hashStart = System.currentTimeMillis();
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);
                    log.debug("[MinIO上传] 创建DigestInputStream耗时: {}ms", System.currentTimeMillis() - hashStart);

                    minioStorageService.uploadChunk(
                            chunk.getBucket(),
                            chunk.getObjectName(),
                            digestInputStream,
                            file.getSize(),
                            "application/octet-stream"
                    );

                    long hashEncodeStart = System.currentTimeMillis();
                    chunkHash = Hex.encodeHexString(digest.digest());
                    log.debug("[MinIO上传] Hash计算耗时: {}ms", System.currentTimeMillis() - hashEncodeStart);
                } else {
                    minioStorageService.uploadChunk(
                            chunk.getBucket(),
                            chunk.getObjectName(),
                            inputStream,
                            file.getSize(),
                            "application/octet-stream"
                    );
                }

                long minioUploadTime = System.currentTimeMillis() - minioUploadStart;
                long totalTime = System.currentTimeMillis() - uploadStart;

                log.info("[MinIO上传-成功] chunkNumber={}, attempt={}, MinIO上传耗时={}ms, 总耗时={}ms, size={} bytes",
                        chunk.getChunkNumber(), attempt, minioUploadTime, totalTime, file.getSize());

                return chunkHash;

            } catch (Exception e) {
                long errorTime = System.currentTimeMillis() - uploadStart;

                if (attempt == maxRetryCount) {
                    log.error("[MinIO上传-失败] chunkNumber={}, attempt={}, 总耗时={}ms, 已达到最大重试次数",
                            chunk.getChunkNumber(), attempt, errorTime, e);
                    throw new FileException(ErrorCode.CHUNK_UPLOAD_FAILED);
                }

                log.warn("[MinIO上传-重试] chunkNumber={}, attempt={}/{}, 耗时={}ms, error={}",
                        chunk.getChunkNumber(), attempt + 1, maxRetryCount, errorTime, e.getMessage());

                try {
                    TimeUnit.SECONDS.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new FileException(ErrorCode.CHUNK_UPLOAD_INTERRUPTED);
                }
            }
        }

        throw new FileException(ErrorCode.CHUNK_UPLOAD_FAILED);
    }

    /**
     * 从合并后的对象计算hash
     */
    private String calculateHashFromMergedObject(String bucket, String objectName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            InputStream inputStream = minioStorageService.download(bucket, objectName);

            DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);
            byte[] buffer = new byte[8192];
            while (digestInputStream.read(buffer) != -1) {
                // 继续读取以更新 digest
            }

            digestInputStream.close();
            return Hex.encodeHexString(digest.digest());
        } catch (Exception e) {
            log.error("计算文件hash失败: bucket={}, objectName={}", bucket, objectName, e);
            throw new FileException(ErrorCode.CHUNK_MERGE_FAILED);
        }
    }

    /**
     * 清理临时分片
     */
    private void cleanupTempChunks(UUID uploadId, List<FileChunk> chunks) {
        try {
            List<String> chunkObjectNames = chunks.stream()
                    .map(FileChunk::getObjectName)
                    .collect(Collectors.toList());

            minioStorageService.deleteChunks(defaultBucket, chunkObjectNames);
            fileChunkRepository.deleteByUploadSessionId(uploadId);

            log.info("清理临时分片成功: uploadId={}, chunkCount={}", uploadId, chunks.size());
        } catch (Exception e) {
            log.error("清理临时分片失败: uploadId={}", uploadId, e);
        }
    }

    private String resolveFileName(UUID ownerId, String originalFileName) {
        String baseName = FilenameUtils.getBaseName(originalFileName);
        String extension = FilenameUtils.getExtension(originalFileName);
        String candidateName = originalFileName;
        int counter = 1;

        while (fileNameExists(ownerId, candidateName)) {
            String suffix = extension.isEmpty() ? "" : "." + extension;
            candidateName = String.format("%s(%d)%s", baseName, counter++, suffix);
        }

        return candidateName;
    }

    private boolean fileNameExists(UUID ownerId, String fileName) {
        return fileRepository.existsByOwnerIdAndFolderIdIsNullAndOriginalFileNameAndStatus(
                ownerId, fileName, FileStatus.ACTIVE);
    }

    /**
     * 生成分片对象名称
     */
    private String generateChunkObjectName(UUID uploadId, int chunkNumber) {
        return String.format("temp/%s/chunk_%d", uploadId.toString(), chunkNumber);
    }

    /**
     * 构建进度响应
     */
    private UnifiedChunkUploadResponse buildProgressResponse(UploadSession session, boolean completed) {
        return UnifiedChunkUploadResponse.builder()
                .uploadId(session.getId())
                .chunkNumber(session.getUploadedChunks())
                .totalChunks(session.getTotalChunks())
                .uploadedChunks(session.getUploadedChunks())
                .progress(calculateProgress(session.getUploadedChunks(), session.getTotalChunks()))
                .completed(completed)
                .message(completed ? "上传完成" : "分片上传中")
                .build();
    }

    /**
     * 构建完成响应（包含文件信息）
     */
    private UnifiedChunkUploadResponse buildCompletedResponse(UploadSession session, File file, String fileHash) {
        return UnifiedChunkUploadResponse.builder()
                .uploadId(session.getId())
                .chunkNumber(session.getTotalChunks())
                .totalChunks(session.getTotalChunks())
                .uploadedChunks(session.getTotalChunks())
                .progress(100.0)
                .completed(true)
                .fileId(file.getId())
                .fileHash(fileHash)
                .message("上传完成")
                .build();
    }

    /**
     * 计算上传进度
     */
    private Double calculateProgress(int uploaded, int total) {
        if (total == 0) return 0.0;
        return (double) uploaded / total * 100.0;
    }

    /**
     * 为统一上传合并分片
     */
    @Transactional
    protected UnifiedChunkUploadResponse mergeChunksForUnifiedUpload(UUID ownerId, UploadSession session) {
        log.info("[合并分片-开始] uploadId={}, fileName={}, totalChunks={}",
                session.getId(), session.getOriginalFileName(), session.getTotalChunks());

        long mergeStartTime = System.currentTimeMillis();

        try {
            // 1. 查询所有已上传的分片
            long queryStart = System.currentTimeMillis();
            List<FileChunk> chunks = fileChunkRepository.findByUploadSessionId(session.getId());
            log.info("[合并分片] 查询分片列表耗时: {}ms, count={}", System.currentTimeMillis() - queryStart, chunks.size());

            if (chunks.size() != session.getTotalChunks()) {
                throw new FileException(ErrorCode.CHUNK_MERGE_FAILED); // 使用已存在的错误码
            }

            // 2. 按分片编号排序
            long sortStart = System.currentTimeMillis();
            chunks.sort(Comparator.comparingInt(FileChunk::getChunkNumber));
            log.debug("[合并分片] 排序分片耗时: {}ms", System.currentTimeMillis() - sortStart);

            // 3. 构建分片对象名称列表
            List<String> chunkObjectNames = chunks.stream()
                    .map(FileChunk::getObjectName)
                    .collect(Collectors.toList());

            // 4. 生成最终文件名（确保不冲突）
            String finalFileName = resolveFileName(ownerId, session.getOriginalFileName());

            // 5. 先计算文件hash用于去重检查
            long hashStart = System.currentTimeMillis();
            
            // 临时合并分片以计算hash
            String tempObjectName = generateFileObjectName(ownerId, "temp_" + UUID.randomUUID().toString());
            minioStorageService.mergeChunks(defaultBucket, chunkObjectNames, tempObjectName);
            String fileHash = calculateHashFromMergedObject(defaultBucket, tempObjectName);
            
            log.info("[合并分片] Hash计算耗时: {}ms, hash={}", System.currentTimeMillis() - hashStart, fileHash);

            // 6. 检查是否存在相同hash的文件（内容去重）
            File existingFile = fileRepository.findByOwnerIdAndFileHashAndStatus(ownerId, fileHash, FileStatus.ACTIVE);
            
            String finalObjectName;
            if (existingFile != null) {
                // 发现重复内容，复用已存在的对象
                log.info("[内容去重] 发现重复文件: existingFileId={}, hash={}, 将复用存储对象", 
                        existingFile.getId(), fileHash);
                finalObjectName = existingFile.getObjectName();
                
                // 删除临时合并的文件
                try {
                    minioStorageService.deleteChunks(defaultBucket, List.of(tempObjectName));
                } catch (Exception e) {
                    log.warn("删除临时文件失败: {}", tempObjectName, e);
                }
            } else {
                // 新文件，使用临时文件并重命名
                finalObjectName = generateFileObjectName(ownerId, finalFileName);
                
                // 如果临时文件名和最终文件名不同，需要复制
                if (!tempObjectName.equals(finalObjectName)) {
                    try {
                        minioStorageService.copyObject(defaultBucket, tempObjectName, defaultBucket, finalObjectName);
                        minioStorageService.deleteChunks(defaultBucket, List.of(tempObjectName));
                    } catch (Exception e) {
                        log.error("重命名文件失败，将使用临时文件名", e);
                        finalObjectName = tempObjectName;
                    }
                }
            }

            // 7. 创建文件记录
            long createFileStart = System.currentTimeMillis();
            File file = File.builder()
                    .ownerId(ownerId)
                    .originalFileName(finalFileName)
                    .objectName(finalObjectName)
                    .size(session.getFileSize())
                    .contentType(session.getContentType())
                    .fileHash(fileHash)
                    .bucket(defaultBucket)
                    .status(FileStatus.ACTIVE)
                    .build();

            fileRepository.save(file);
            log.info("[合并分片] 创建文件记录耗时: {}ms, fileId={}, isDuplicate={}", 
                    System.currentTimeMillis() - createFileStart, file.getId(), existingFile != null);

            // 8. 更新会话状态
            long updateSessionStart = System.currentTimeMillis();
            uploadSessionRepository.updateUploadedChunksAndStatus(
                    session.getId(),
                    session.getTotalChunks(),
                    UploadStatus.COMPLETED
            );
            log.info("[合并分片] 更新会话状态耗时: {}ms", System.currentTimeMillis() - updateSessionStart);

            // 9. 清理临时分片（异步，不阻塞响应）
            long cleanupStart = System.currentTimeMillis();
            cleanupTempChunks(session.getId(), chunks);
            log.info("[合并分片] 清理临时分片耗时: {}ms", System.currentTimeMillis() - cleanupStart);

            long totalTime = System.currentTimeMillis() - mergeStartTime;
            log.info("[合并分片-完成] uploadId={}, 总耗时={}ms, fileId={}, 内容去重={}", 
                    session.getId(), totalTime, file.getId(), existingFile != null);

            // 10. 返回完成响应
            return buildCompletedResponse(session, file, fileHash);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - mergeStartTime;
            log.error("[合并分片-失败] uploadId={}, 耗时={}ms", session.getId(), totalTime, e);

            // 更新会话状态为失败
            uploadSessionRepository.updateUploadedChunksAndStatus(
                    session.getId(),
                    session.getUploadedChunks(),
                    UploadStatus.FAILED
            );

            throw new FileException(ErrorCode.CHUNK_MERGE_FAILED);
        }
    }

    /**
     * 生成文件对象名称
     */
    private String generateFileObjectName(UUID ownerId, String fileName) {
        return String.format("files/%s/%s", ownerId.toString(), fileName);
    }
}
