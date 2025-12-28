package com.evorsio.mybox.file.service;

import com.evorsio.mybox.file.dto.FileConfigResponse;
import com.evorsio.mybox.file.dto.FileConfigUpdateRequest;
import com.evorsio.mybox.file.properties.FileDownloadProperties;
import com.evorsio.mybox.file.properties.FileStorageProperties;
import com.evorsio.mybox.file.properties.FileUploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件配置服务
 * 负责管理文件上传和下载的配置
 * 返回格式完全按照 YAML 配置层次设计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileConfigService {
    private final FileStorageProperties fileStorageProperties;
    private final FileUploadProperties fileUploadProperties;
    private final FileDownloadProperties fileDownloadProperties;

    // 运行时配置缓存（用于动态更新）
    private final Map<String, Object> runtimeConfig = new HashMap<>();

    /**
     * 获取文件配置（只读）
     * 返回格式完全按照 YAML 配置层次
     */
    public FileConfigResponse getFileConfig() {
        // Storage 配置
        FileConfigResponse.StorageConfig storage = FileConfigResponse.StorageConfig.builder()
                .tempDir(getRuntimeConfig("storage.tempDir", fileStorageProperties.getTempDir()))
                .maxFileSize(fileStorageProperties.getMaxFileSize())
                .maxFileSizeBytes(getRuntimeConfig("storage.maxFileSizeBytes", parseSize(fileStorageProperties.getMaxFileSize())))
                .maxTotalSize(fileStorageProperties.getMaxTotalSize())
                .maxTotalSizeBytes(getRuntimeConfig("storage.maxTotalSizeBytes", parseSize(fileStorageProperties.getMaxTotalSize())))
                .retention(fileStorageProperties.getRetention())
                .retentionDays(getRuntimeConfig("storage.retentionDays", parseTime(fileStorageProperties.getRetention()) / 24))
                .build();

        // Upload.Types 配置
        FileConfigResponse.UploadConfig.TypesConfig types = FileConfigResponse.UploadConfig.TypesConfig.builder()
                .allowAll(getRuntimeConfig("upload.types.allowAll", fileUploadProperties.getTypes().isAllowAll()))
                .allowedContentTypes(fileUploadProperties.getTypes().getAllowedContentTypes())
                .allowedExtensions(fileUploadProperties.getTypes().getAllowedExtensions())
                .build();

        // Upload.Chunk 配置
        FileUploadProperties.Chunk chunkProps = fileUploadProperties.getChunk();
        FileConfigResponse.UploadConfig.ChunkConfig chunk = FileConfigResponse.UploadConfig.ChunkConfig.builder()
                .enabled(getRuntimeConfig("upload.chunk.enabled", chunkProps.isEnabled()))
                .minSize(chunkProps.getMinSize())
                .minSizeBytes(getRuntimeConfig("upload.chunk.minSizeBytes", parseSize(chunkProps.getMinSize())))
                .chunkSize(chunkProps.getChunkSize())
                .chunkSizeBytes(getRuntimeConfig("upload.chunk.chunkSizeBytes", parseSize(chunkProps.getChunkSize())))
                .enableConcurrent(getRuntimeConfig("upload.chunk.enableConcurrent", chunkProps.isEnableConcurrent()))
                .maxConcurrent(getRuntimeConfig("upload.chunk.maxConcurrent", chunkProps.getMaxConcurrent()))
                .enableDeduplication(getRuntimeConfig("upload.chunk.enableDeduplication", chunkProps.isEnableDeduplication()))
                .deduplicationStrategy(getRuntimeConfig("upload.chunk.deduplicationStrategy", chunkProps.getDeduplicationStrategy()))
                .sessionTimeout(chunkProps.getSessionTimeout())
                .sessionTimeoutHours(getRuntimeConfig("upload.chunk.sessionTimeoutHours", parseTime(chunkProps.getSessionTimeout())))
                .maxRetry(getRuntimeConfig("upload.chunk.maxRetry", chunkProps.getMaxRetry()))
                .retryDelay(chunkProps.getRetryDelay())
                .retryDelaySeconds(getRuntimeConfig("upload.chunk.retryDelaySeconds", parseTime(chunkProps.getRetryDelay())))
                .cleanupExpired(getRuntimeConfig("upload.chunk.cleanupExpired", chunkProps.isCleanupExpired()))
                .cleanupInterval(chunkProps.getCleanupInterval())
                .cleanupIntervalHours(getRuntimeConfig("upload.chunk.cleanupIntervalHours", parseTime(chunkProps.getCleanupInterval())))
                .build();

        // Upload 配置
        FileConfigResponse.UploadConfig upload = FileConfigResponse.UploadConfig.builder()
                .types(types)
                .chunk(chunk)
                .build();

        // Download 配置
        FileConfigResponse.DownloadConfig download = FileConfigResponse.DownloadConfig.builder()
                .enableRateLimit(getRuntimeConfig("download.enableRateLimit", fileDownloadProperties.isEnableRateLimit()))
                .maxBytesPerSecond(fileDownloadProperties.getMaxBytesPerSecond())
                .maxBytesPerSecondBytes(getRuntimeConfig("download.maxBytesPerSecondBytes", parseSize(fileDownloadProperties.getMaxBytesPerSecond())))
                .build();

        return FileConfigResponse.builder()
                .storage(storage)
                .upload(upload)
                .download(download)
                .build();
    }

    /**
     * 更新文件配置（仅Admin）
     */
    public void updateFileConfig(FileConfigUpdateRequest request) {
        log.info("更新文件配置: {}", request);

        // Storage 配置
        runtimeConfig.put("storage.tempDir", request.getStorage().getTempDir());
        runtimeConfig.put("storage.maxFileSizeBytes", request.getStorage().getMaxFileSizeBytes());
        runtimeConfig.put("storage.maxTotalSizeBytes", request.getStorage().getMaxTotalSizeBytes());
        runtimeConfig.put("storage.retentionDays", request.getStorage().getRetentionDays());

        // Upload.Types 配置
        runtimeConfig.put("upload.types.allowAll", request.getUpload().getTypes().getAllowAll());

        // Upload.Chunk 配置
        runtimeConfig.put("upload.chunk.enabled", request.getUpload().getChunk().getEnabled());
        runtimeConfig.put("upload.chunk.minSizeBytes", request.getUpload().getChunk().getMinSizeBytes());
        runtimeConfig.put("upload.chunk.chunkSizeBytes", request.getUpload().getChunk().getChunkSizeBytes());
        runtimeConfig.put("upload.chunk.enableConcurrent", request.getUpload().getChunk().getEnableConcurrent());
        runtimeConfig.put("upload.chunk.maxConcurrent", request.getUpload().getChunk().getMaxConcurrent());
        runtimeConfig.put("upload.chunk.enableDeduplication", request.getUpload().getChunk().getEnableDeduplication());
        runtimeConfig.put("upload.chunk.deduplicationStrategy", request.getUpload().getChunk().getDeduplicationStrategy());
        runtimeConfig.put("upload.chunk.sessionTimeoutHours", request.getUpload().getChunk().getSessionTimeoutHours());
        runtimeConfig.put("upload.chunk.maxRetry", request.getUpload().getChunk().getMaxRetry());
        runtimeConfig.put("upload.chunk.retryDelaySeconds", request.getUpload().getChunk().getRetryDelaySeconds());
        runtimeConfig.put("upload.chunk.cleanupExpired", request.getUpload().getChunk().getCleanupExpired());
        runtimeConfig.put("upload.chunk.cleanupIntervalHours", request.getUpload().getChunk().getCleanupIntervalHours());

        // Download 配置
        runtimeConfig.put("download.enableRateLimit", request.getDownload().getEnableRateLimit());
        runtimeConfig.put("download.maxBytesPerSecondBytes", request.getDownload().getMaxBytesPerSecondBytes());

        log.info("文件配置更新成功");
    }

    /**
     * 获取运行时配置值（用于动态更新）
     */
    @SuppressWarnings("unchecked")
    private <T> T getRuntimeConfig(String key, T defaultValue) {
        Object value = runtimeConfig.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 解析大小字符串（如 "2GB", "100MB"）为字节数
     */
    private long parseSize(String size) {
        if (size == null || size.isEmpty()) {
            return 0;
        }
        size = size.trim().toUpperCase();
        try {
            if (size.endsWith("GB")) {
                return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024 * 1024;
            } else if (size.endsWith("MB")) {
                return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024;
            } else if (size.endsWith("KB")) {
                return Long.parseLong(size.substring(0, size.length() - 2)) * 1024;
            } else if (size.endsWith("B")) {
                return Long.parseLong(size.substring(0, size.length() - 1));
            } else {
                return Long.parseLong(size);
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析大小配置: {}", size);
            return 0;
        }
    }

    /**
     * 解析时间字符串（如 "24h", "5s"）为数值
     */
    private int parseTime(String time) {
        if (time == null || time.isEmpty()) {
            return 0;
        }
        time = time.trim().toLowerCase();
        try {
            int parseInt = Integer.parseInt(time.substring(0, time.length() - 1));
            if (time.endsWith("h")) {
                return parseInt;
            } else if (time.endsWith("s")) {
                return parseInt;
            } else if (time.endsWith("m")) {
                return parseInt * 60;
            } else {
                return Integer.parseInt(time);
            }
        } catch (NumberFormatException e) {
            log.warn("无法解析时间配置: {}", time);
            return 0;
        }
    }

    // ========== 以下是用于其他 Service 调用的便捷方法 ==========

    /**
     * 获取临时文件目录
     */
    public String getTempDir() {
        String cached = getRuntimeConfig("storage.tempDir", null);
        if (cached != null) {
            return cached;
        }
        return fileStorageProperties.getTempDir();
    }

    /**
     * 获取最大文件大小（字节）
     */
    public long getMaxFileSize() {
        Long cached = getRuntimeConfig("storage.maxFileSizeBytes", null);
        if (cached != null) {
            return cached;
        }
        return parseSize(fileStorageProperties.getMaxFileSize());
    }

    /**
     * 获取最大总存储大小（字节）
     */
    public long getMaxTotalStorageSize() {
        Long cached = getRuntimeConfig("storage.maxTotalSizeBytes", null);
        if (cached != null) {
            return cached;
        }
        return parseSize(fileStorageProperties.getMaxTotalSize());
    }

    /**
     * 获取文件保留时间（小时）
     */
    public int getRetentionHours() {
        Integer cached = getRuntimeConfig("storage.retentionDays", null);
        if (cached != null) {
            return cached * 24;
        }
        return parseTime(fileStorageProperties.getRetention());
    }

    /**
     * 是否允许所有文件类型
     */
    public boolean isAllowAllFileTypes() {
        return getRuntimeConfig("upload.types.allowAll", fileUploadProperties.getTypes().isAllowAll());
    }

    /**
     * 检查文件扩展名是否被拒绝
     */
    public boolean isExtensionRejected(String extension) {
        boolean allowAll = isAllowAllFileTypes();
        if (allowAll) {
            return false;
        }
        return !fileUploadProperties.getTypes().getAllowedExtensions().contains(extension);
    }

    /**
     * 检查内容类型是否被拒绝
     */
    public boolean isContentTypeRejected(String contentType) {
        boolean allowAll = isAllowAllFileTypes();
        if (allowAll) {
            return false;
        }
        return !fileUploadProperties.getTypes().getAllowedContentTypes().contains(contentType);
    }

    /**
     * 获取允许的文件扩展名
     */
    public java.util.List<String> getAllowedExtensions() {
        return fileUploadProperties.getTypes().getAllowedExtensions();
    }

    /**
     * 获取允许的内容类型
     */
    public java.util.List<String> getAllowedContentTypes() {
        return fileUploadProperties.getTypes().getAllowedContentTypes();
    }

    /**
     * 是否启用分片上传
     */
    public boolean isChunkUploadEnabled() {
        return getRuntimeConfig("upload.chunk.enabled", fileUploadProperties.getChunk().isEnabled());
    }

    /**
     * 获取分片上传最小文件大小（字节）
     */
    public long getChunkUploadMinSize() {
        Long cached = getRuntimeConfig("upload.chunk.minSizeBytes", null);
        if (cached != null) {
            return cached;
        }
        return parseSize(fileUploadProperties.getChunk().getMinSize());
    }

    /**
     * 获取分片大小（字节）
     */
    public long getChunkUploadChunkSize() {
        Long cached = getRuntimeConfig("upload.chunk.chunkSizeBytes", null);
        if (cached != null) {
            return cached;
        }
        return parseSize(fileUploadProperties.getChunk().getChunkSize());
    }

    /**
     * 获取分片上传会话超时时间（小时）
     */
    public int getChunkUploadSessionTimeoutHours() {
        Integer cached = getRuntimeConfig("upload.chunk.sessionTimeoutHours", null);
        if (cached != null) {
            return cached;
        }
        return parseTime(fileUploadProperties.getChunk().getSessionTimeout());
    }

    /**
     * 获取分片上传最大重试次数
     */
    public int getChunkUploadMaxRetryCount() {
        Integer cached = getRuntimeConfig("upload.chunk.maxRetry", null);
        if (cached != null) {
            return cached;
        }
        return fileUploadProperties.getChunk().getMaxRetry();
    }

    /**
     * 获取分片上传重试延迟（秒）
     */
    public int getChunkUploadRetryDelaySeconds() {
        Integer cached = getRuntimeConfig("upload.chunk.retryDelaySeconds", null);
        if (cached != null) {
            return cached;
        }
        return parseTime(fileUploadProperties.getChunk().getRetryDelay());
    }

    /**
     * 是否启用分片上传去重
     */
    public boolean isChunkUploadDeduplicationEnabled() {
        return getRuntimeConfig("upload.chunk.enableDeduplication", fileUploadProperties.getChunk().isEnableDeduplication());
    }

    /**
     * 获取分片上传去重策略
     */
    public String getChunkUploadDeduplicationStrategy() {
        return getRuntimeConfig("upload.chunk.deduplicationStrategy", fileUploadProperties.getChunk().getDeduplicationStrategy());
    }

    /**
     * 获取最大下载速度（字节/秒）
     */
    public long getMaxDownloadSpeed() {
        Long cached = getRuntimeConfig("download.maxBytesPerSecondBytes", null);
        if (cached != null) {
            return cached;
        }
        return parseSize(fileDownloadProperties.getMaxBytesPerSecond());
    }
}
