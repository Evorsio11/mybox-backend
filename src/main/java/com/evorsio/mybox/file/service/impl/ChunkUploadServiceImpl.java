package com.evorsio.mybox.file.service.impl;

import com.evorsio.mybox.common.error.ErrorCode;
import com.evorsio.mybox.file.domain.*;
import com.evorsio.mybox.file.dto.ChunkInitRequest;
import com.evorsio.mybox.file.dto.ChunkInitResponse;
import com.evorsio.mybox.file.dto.ChunkMergeResponse;
import com.evorsio.mybox.file.dto.ChunkProgressResponse;
import com.evorsio.mybox.file.dto.ChunkResumeResponse;
import com.evorsio.mybox.file.dto.UploadChunkRequest;
import com.evorsio.mybox.file.dto.UploadChunkResponse;
import com.evorsio.mybox.file.exception.FileException;
import com.evorsio.mybox.file.repository.FileChunkRepository;
import com.evorsio.mybox.file.repository.FileRepository;
import com.evorsio.mybox.file.repository.UploadSessionRepository;
import com.evorsio.mybox.file.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public ChunkInitResponse initUpload(UUID ownerId, ChunkInitRequest request) {
        // 1. 检查分片上传功能是否启用
        if (!fileConfigService.isChunkUploadEnabled()) {
            throw new FileException(ErrorCode.CHUNK_UPLOAD_DISABLED);
        }

        // 2. 校验文件大小
        if (request.getFileSize() > fileConfigService.getMaxFileSize()) {
            throw new FileException(ErrorCode.FILE_TOO_LARGE);
        }

        if (request.getFileSize() < fileConfigService.getChunkUploadMinSize()) {
            throw new FileException(ErrorCode.FILE_TOO_LARGE_FOR_CHUNK);
        }

        // 3. 校验存储空间
        long usedSize = fileRepository.sumActiveFileSizeByOwnerId(ownerId, FileStatus.ACTIVE);
        if (usedSize + request.getFileSize() > fileConfigService.getMaxTotalStorageSize()) {
            throw new FileException(ErrorCode.STORAGE_FULL);
        }

        // 4. 校验文件类型
        String ext = FilenameUtils.getExtension(request.getOriginalFileName()).toLowerCase();
        if (fileConfigService.isExtensionRejected(ext)) {
            log.warn("文件扩展名不允许: {}, allowAllFileTypes={}", ext,
                    fileConfigService.isAllowAllFileTypes());
            throw new FileException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        if (fileConfigService.isContentTypeRejected(request.getContentType())) {
            log.warn("文件 MIME 类型不允许: {}, allowAllFileTypes={}", request.getContentType(),
                    fileConfigService.isAllowAllFileTypes());
            throw new FileException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        // 5. 幂等性检查：查找最近30秒内是否有相同文件的上传会话（在事务外）
        LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);
        List<UploadSession> recentSessions = uploadSessionRepository.findRecentSessionsByFile(
                ownerId,
                request.getOriginalFileName(),
                request.getFileSize(),
                thirtySecondsAgo
        );

        // 如果找到进行中或刚初始化的会话，直接返回（幂等性）
        for (UploadSession existingSession : recentSessions) {
            if (existingSession.getStatus() == UploadStatus.INIT ||
                existingSession.getStatus() == UploadStatus.UPLOADING) {
                log.info("发现重复上传请求，返回已有会话: existingUploadId={}, fileName={}",
                        existingSession.getId(), request.getOriginalFileName());

                return ChunkInitResponse.builder()
                        .uploadId(existingSession.getId())
                        .chunkSize(existingSession.getChunkSize())
                        .totalChunks(existingSession.getTotalChunks())
                        .expiresAt(existingSession.getExpiresAt())
                        .build();
            }
        }

        // 6. 在新事务中创建上传会话
        return createUploadSession(ownerId, request);
    }

    /**
     * 在新事务中创建上传会话
     */
    @Transactional
    protected ChunkInitResponse createUploadSession(UUID ownerId, ChunkInitRequest request) {
        // 计算分片信息
        long chunkSize = fileConfigService.getChunkUploadChunkSize();
        int totalChunks = (int) Math.ceil((double) request.getFileSize() / chunkSize);

        try {
            UploadSession session = UploadSession.builder()
                    .ownerId(ownerId)
                    .originalFileName(request.getOriginalFileName())
                    .fileSize(request.getFileSize())
                    .contentType(request.getContentType())
                    .totalChunks(totalChunks)
                    .uploadedChunks(0)
                    .chunkSize(chunkSize)
                    .status(UploadStatus.INIT)
                    .bucket(defaultBucket)
                    .expiresAt(LocalDateTime.now().plusHours(fileConfigService.getChunkUploadSessionTimeoutHours()))
                    .build();

            uploadSessionRepository.save(session);

            // 预创建分片记录
            List<FileChunk> chunks = IntStream.rangeClosed(1, totalChunks)
                    .mapToObj(i -> FileChunk.builder()
                            .uploadSessionId(session.getId())
                            .chunkNumber(i)
                            .chunkSize(chunkSize)
                            .bucket(defaultBucket)
                            .objectName(generateChunkObjectName(session.getId(), i))
                            .status(ChunkStatus.PENDING)
                            .build())
                    .collect(Collectors.toList());

            fileChunkRepository.saveAll(chunks);

            log.info("初始化分片上传成功: uploadId={}, ownerId={}, fileName={}, totalChunks={}",
                    session.getId(), ownerId, request.getOriginalFileName(), totalChunks);

            return ChunkInitResponse.builder()
                    .uploadId(session.getId())
                    .chunkSize(chunkSize)
                    .totalChunks(totalChunks)
                    .expiresAt(session.getExpiresAt())
                    .build();

        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
            // 唯一键冲突或乐观锁冲突：说明其他请求已经创建了session
            log.warn("检测到并发创建，复用已有会话: fileName={}, error={}",
                    request.getOriginalFileName(), e.getClass().getSimpleName());

            // 查询已有的会话（不限制时间窗口）
            List<UploadSession> existingSessions = uploadSessionRepository.findSessionsByFile(
                    ownerId,
                    request.getOriginalFileName(),
                    request.getFileSize()
            );

            if (!existingSessions.isEmpty()) {
                // 优先选择进行中的session（INIT或UPLOADING状态）
                UploadSession existingSession = existingSessions.stream()
                        .filter(s -> s.getStatus() == UploadStatus.INIT || s.getStatus() == UploadStatus.UPLOADING)
                        .findFirst()
                        .orElse(existingSessions.get(0)); // 如果没有进行中的，使用第一个

                log.info("复用已有上传会话: uploadId={}, status={}", existingSession.getId(), existingSession.getStatus());
                return ChunkInitResponse.builder()
                        .uploadId(existingSession.getId())
                        .chunkSize(existingSession.getChunkSize())
                        .totalChunks(existingSession.getTotalChunks())
                        .expiresAt(existingSession.getExpiresAt())
                        .build();
            }

            // 如果还是找不到，说明有问题
            log.error("并发冲突但找不到已有会话: fileName={}", request.getOriginalFileName(), e);
            throw new FileException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    @Transactional
    public UploadChunkResponse uploadChunk(UUID ownerId, UploadChunkRequest request) {
        // 1. 查询上传会话
        UploadSession session = uploadSessionRepository.findByIdAndOwnerId(request.getUploadId(), ownerId)
                .orElseThrow(() -> new FileException(ErrorCode.UPLOAD_SESSION_NOT_FOUND));

        // 2. 校验会话状态
        if (session.getStatus() == UploadStatus.EXPIRED) {
            throw new FileException(ErrorCode.UPLOAD_SESSION_EXPIRED);
        }

        if (session.getStatus() == UploadStatus.COMPLETED) {
            throw new FileException(ErrorCode.CHUNK_UPLOAD_INCOMPLETE);
        }

        // 3. 校验分片编号
        if (request.getChunkNumber() < 1 || request.getChunkNumber() > session.getTotalChunks()) {
            throw new FileException(ErrorCode.CHUNK_NUMBER_INVALID);
        }

        // 4. 查询或创建分片记录（处理并发初始化的情况）
        FileChunk chunk = fileChunkRepository.findByUploadSessionIdAndChunkNumber(
                request.getUploadId(), request.getChunkNumber())
                .orElseGet(() -> {
                    // 如果分片记录不存在，自动创建（可能是因为初始化时事务回滚）
                    log.info("分片记录不存在，自动创建: uploadId={}, chunkNumber={}",
                            request.getUploadId(), request.getChunkNumber());

                    FileChunk newChunk = FileChunk.builder()
                            .uploadSessionId(request.getUploadId())
                            .chunkNumber(request.getChunkNumber())
                            .chunkSize(session.getChunkSize())
                            .bucket(session.getBucket())
                            .objectName(generateChunkObjectName(request.getUploadId(), request.getChunkNumber()))
                            .status(ChunkStatus.PENDING)
                            .build();

                    return fileChunkRepository.save(newChunk);
                });

        // 5. 如果分片已上传，直接返回成功
        if (chunk.getStatus() == ChunkStatus.COMPLETED) {
            log.info("分片已上传，跳过: uploadId={}, chunkNumber={}",
                    request.getUploadId(), request.getChunkNumber());
            return UploadChunkResponse.builder()
                    .chunkNumber(request.getChunkNumber())
                    .status("COMPLETED")
                    .chunkHash(chunk.getChunkHash())
                    .build();
        }

        // 6. 并发控制
        if (!concurrencyManager.tryAcquire(request.getUploadId())) {
            throw new FileException(ErrorCode.CONCURRENT_UPLOAD_LIMIT_EXCEEDED);
        }

        try {
            // 7. 上传分片（带重试）
            String chunkHash = uploadChunkWithRetry(request, chunk);

            // 8. 更新分片状态
            fileChunkRepository.updateChunkStatus(
                    chunk.getId(),
                    ChunkStatus.COMPLETED,
                    chunkHash,
                    LocalDateTime.now()
            );

            // 9. 更新会话状态和已上传分片数
            long uploadedCount = fileChunkRepository.countByUploadSessionIdAndStatus(
                    request.getUploadId(), ChunkStatus.COMPLETED);

            uploadSessionRepository.updateUploadedChunksAndStatus(
                    request.getUploadId(),
                    (int) uploadedCount,
                    UploadStatus.UPLOADING
            );

            log.info("分片上传成功: uploadId={}, chunkNumber={}, uploaded={}/{}",
                    request.getUploadId(), request.getChunkNumber(), uploadedCount, session.getTotalChunks());

            return UploadChunkResponse.builder()
                    .chunkNumber(request.getChunkNumber())
                    .status("COMPLETED")
                    .chunkHash(chunkHash)
                    .build();
        } finally {
            concurrencyManager.release(request.getUploadId());
        }
    }

    @Override
    @Transactional
    public ChunkMergeResponse mergeChunks(UUID ownerId, UUID uploadId) {
        // 1. 查询上传会话
        UploadSession session = uploadSessionRepository.findByIdAndOwnerId(uploadId, ownerId)
                .orElseThrow(() -> new FileException(ErrorCode.UPLOAD_SESSION_NOT_FOUND));

        // 2. 校验所有分片是否已上传
        long completedCount = fileChunkRepository.countByUploadSessionIdAndStatus(uploadId, ChunkStatus.COMPLETED);
        if (completedCount < session.getTotalChunks()) {
            throw new FileException(ErrorCode.CHUNK_UPLOAD_INCOMPLETE);
        }

        try {
            // 3. 获取所有分片
            List<FileChunk> chunks = fileChunkRepository.findByUploadSessionId(uploadId);
            chunks.sort(Comparator.comparing(FileChunk::getChunkNumber));

            // 4. 在 MinIO 中合并分片
            String targetObjectName = UUID.randomUUID().toString();
            List<String> chunkObjectNames = chunks.stream()
                    .map(FileChunk::getObjectName)
                    .collect(Collectors.toList());

            minioStorageService.mergeChunks(defaultBucket, chunkObjectNames, targetObjectName);

            // 5. 计算完整文件hash（从合并后的文件）
            String fileHash = calculateHashFromMergedObject(defaultBucket, targetObjectName);

            // 6. 去重检查
            com.evorsio.mybox.file.domain.File existingFile = fileRepository.findByOwnerIdAndFileHashAndStatus(
                    ownerId, fileHash, FileStatus.ACTIVE);

            if (existingFile != null) {
                // 文件重复，删除刚合并的文件和临时分片
                cleanupTempChunks(uploadId, chunks);
                minioStorageService.delete(defaultBucket, targetObjectName);

                log.info("文件已存在，返回已有文件: uploadId={}, existingFileId={}",
                        uploadId, existingFile.getId());

                return ChunkMergeResponse.builder()
                        .fileId(existingFile.getId())
                        .originalFileName(existingFile.getOriginalFileName())
                        .fileSize(existingFile.getSize())
                        .fileHash(existingFile.getFileHash())
                        .build();
            }

            // 7. 创建文件记录
            com.evorsio.mybox.file.domain.File file = new com.evorsio.mybox.file.domain.File();
            file.setOwnerId(ownerId);
            file.setOriginalFileName(session.getOriginalFileName());
            file.setObjectName(targetObjectName);
            file.setBucket(defaultBucket);
            file.setContentType(session.getContentType());
            file.setSize(session.getFileSize());
            file.setFileHash(fileHash);
            file.setStatus(FileStatus.ACTIVE);

            com.evorsio.mybox.file.domain.File savedFile = fileRepository.save(file);

            // 8. 清理临时分片
            cleanupTempChunks(uploadId, chunks);

            // 9. 更新会话状态
            session.setStatus(UploadStatus.COMPLETED);
            session.setFileHash(fileHash);
            uploadSessionRepository.save(session);

            log.info("分片合并成功: uploadId={}, fileId={}, fileSize={}",
                    uploadId, savedFile.getId(), savedFile.getSize());

            return ChunkMergeResponse.builder()
                    .fileId(savedFile.getId())
                    .originalFileName(savedFile.getOriginalFileName())
                    .fileSize(savedFile.getSize())
                    .fileHash(savedFile.getFileHash())
                    .build();

        } catch (Exception e) {
            log.error("分片合并失败: uploadId={}", uploadId, e);
            throw new FileException(ErrorCode.CHUNK_MERGE_FAILED);
        }
    }

    @Override
    @Transactional
    public void cancelUpload(UUID ownerId, UUID uploadId) {
        // 1. 查询上传会话
        UploadSession session = uploadSessionRepository.findByIdAndOwnerId(uploadId, ownerId)
                .orElseThrow(() -> new FileException(ErrorCode.UPLOAD_SESSION_NOT_FOUND));

        // 2. 获取所有分片
        List<FileChunk> chunks = fileChunkRepository.findByUploadSessionId(uploadId);

        // 3. 删除 MinIO 中的临时分片
        List<String> chunkObjectNames = chunks.stream()
                .map(FileChunk::getObjectName)
                .collect(Collectors.toList());

        try {
            minioStorageService.deleteChunks(defaultBucket, chunkObjectNames);
        } catch (Exception e) {
            log.error("删除临时分片失败: uploadId={}", uploadId, e);
        }

        // 4. 删除分片记录和会话
        fileChunkRepository.deleteByUploadSessionId(uploadId);
        session.setStatus(UploadStatus.CANCELLED);
        uploadSessionRepository.save(session);

        log.info("取消上传成功: uploadId={}", uploadId);
    }

    @Override
    public ChunkProgressResponse getProgress(UUID ownerId, UUID uploadId) {
        // 1. 查询上传会话
        UploadSession session = uploadSessionRepository.findByIdAndOwnerId(uploadId, ownerId)
                .orElseThrow(() -> new FileException(ErrorCode.UPLOAD_SESSION_NOT_FOUND));

        // 2. 统计已上传分片
        long uploadedCount = fileChunkRepository.countByUploadSessionIdAndStatus(uploadId, ChunkStatus.COMPLETED);
        long uploadedBytes = uploadedCount * session.getChunkSize();

        // 3. 计算进度
        double progress = (double) uploadedCount / session.getTotalChunks() * 100;

        return ChunkProgressResponse.builder()
                .uploadId(session.getId())
                .status(session.getStatus().name())
                .totalChunks(session.getTotalChunks())
                .uploadedChunks((int) uploadedCount)
                .uploadedBytes(uploadedBytes)
                .progress(progress)
                .build();
    }

    @Override
    public ChunkResumeResponse resumeUpload(UUID ownerId, UUID uploadId) {
        // 1. 查询上传会话
        UploadSession session = uploadSessionRepository.findByIdAndOwnerId(uploadId, ownerId)
                .orElseThrow(() -> new FileException(ErrorCode.UPLOAD_SESSION_NOT_FOUND));

        // 2. 校验会话是否过期
        if (session.getStatus() == UploadStatus.EXPIRED ||
                (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now()))) {
            throw new FileException(ErrorCode.UPLOAD_SESSION_EXPIRED);
        }

        // 3. 获取所有分片状态
        List<FileChunk> allChunks = fileChunkRepository.findByUploadSessionId(uploadId);

        List<Integer> uploadedChunks = allChunks.stream()
                .filter(c -> c.getStatus() == ChunkStatus.COMPLETED)
                .map(FileChunk::getChunkNumber)
                .collect(Collectors.toList());

        List<Integer> pendingChunks = allChunks.stream()
                .filter(c -> c.getStatus() != ChunkStatus.COMPLETED)
                .map(FileChunk::getChunkNumber)
                .collect(Collectors.toList());

        return ChunkResumeResponse.builder()
                .uploadId(session.getId())
                .originalFileName(session.getOriginalFileName())
                .totalChunks(session.getTotalChunks())
                .uploadedChunks(uploadedChunks)
                .pendingChunks(pendingChunks)
                .build();
    }

    // ============ 私有辅助方法 ============

    /**
     * 上传分片（带重试）
     */
    private String uploadChunkWithRetry(UploadChunkRequest request, FileChunk chunk) {
        int maxRetryCount = fileConfigService.getChunkUploadMaxRetryCount();
        int retryDelay = fileConfigService.getChunkUploadRetryDelaySeconds();
        String chunkHash = null;

        for (int attempt = 0; attempt <= maxRetryCount; attempt++) {
            try {
                MultipartFile file = request.getFile();
                InputStream inputStream = file.getInputStream();

                // 如果启用了分片去重，计算 hash
                if (fileConfigService.isChunkUploadDeduplicationEnabled() &&
                        "chunk".equals(fileConfigService.getChunkUploadDeduplicationStrategy())) {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);
                    minioStorageService.uploadChunk(
                            chunk.getBucket(),
                            chunk.getObjectName(),
                            digestInputStream,
                            file.getSize(),
                            "application/octet-stream"
                    );
                    chunkHash = Hex.encodeHexString(digest.digest());
                } else {
                    minioStorageService.uploadChunk(
                            chunk.getBucket(),
                            chunk.getObjectName(),
                            inputStream,
                            file.getSize(),
                            "application/octet-stream"
                    );
                }

                return chunkHash;

            } catch (Exception e) {
                if (attempt == maxRetryCount) {
                    log.error("分片上传失败，已达到最大重试次数: chunkNumber={}, attempt={}",
                            request.getChunkNumber(), attempt, e);
                    throw new FileException(ErrorCode.CHUNK_UPLOAD_FAILED);
                }

                log.warn("分片上传失败，准备重试: chunkNumber={}, attempt={}/{}",
                        request.getChunkNumber(), attempt + 1, maxRetryCount, e);

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

    /**
     * 生成分片对象名称
     */
    private String generateChunkObjectName(UUID uploadId, int chunkNumber) {
        return String.format("temp/%s/chunk_%d", uploadId.toString(), chunkNumber);
    }
}
