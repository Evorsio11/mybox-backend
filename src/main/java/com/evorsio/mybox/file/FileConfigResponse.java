package com.evorsio.mybox.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件配置响应DTO（只读）
 * 结构完全按照 YAML 配置层次设计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileConfigResponse {
    private StorageConfig storage;
    private UploadConfig upload;
    private DownloadConfig download;

    /**
     * 存储配置 (file.storage)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageConfig {
        private String tempDir;                 // 临时文件目录
        private String maxFileSize;             // 最大文件大小（原始字符串，如 "2GB"）
        private long maxFileSizeBytes;          // 最大文件大小（字节）
        private String maxTotalSize;            // 最大总存储大小（原始字符串，如 "10GB"）
        private long maxTotalSizeBytes;         // 最大总存储大小（字节）
        private String retention;               // 文件保留时间（原始字符串，如 "24h"）
        private int retentionDays;              // 文件保留天数
    }

    /**
     * 上传配置 (file.upload)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadConfig {
        private TypesConfig types;
        private ChunkConfig chunk;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TypesConfig {
            private boolean allowAll;                        // 是否允许所有文件类型
            private java.util.List<String> allowedContentTypes;   // 允许的内容类型
            private java.util.List<String> allowedExtensions;     // 允许的文件扩展名
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ChunkConfig {
            private boolean enabled;                         // 是否启用分片上传
            private String minSize;                          // 最小文件大小（原始字符串，如 "100MB"）
            private long minSizeBytes;                       // 最小文件大小（字节）
            private String chunkSize;                        // 分片大小（原始字符串，如 "5MB"）
            private long chunkSizeBytes;                     // 分片大小（字节）
            private boolean enableConcurrent;                // 是否启用并发上传
            private int maxConcurrent;                       // 最大并发数
            private boolean enableDeduplication;             // 是否启用去重
            private String deduplicationStrategy;            // 去重策略
            private String sessionTimeout;                   // 会话超时时间（原始字符串，如 "24h"）
            private int sessionTimeoutHours;                 // 会话超时时间（小时）
            private int maxRetry;                            // 最大重试次数
            private String retryDelay;                       // 重试延迟（原始字符串，如 "5s"）
            private int retryDelaySeconds;                   // 重试延迟（秒）
            private boolean cleanupExpired;                  // 是否清理过期会话
            private String cleanupInterval;                  // 清理间隔（原始字符串，如 "1h"）
            private int cleanupIntervalHours;                // 清理间隔（小时）
        }
    }

    /**
     * 下载配置 (file.download)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadConfig {
        private boolean enableRateLimit;              // 是否启用下载限速
        private String maxBytesPerSecond;             // 最大下载速度（原始字符串，如 "500KB"）
        private long maxBytesPerSecondBytes;          // 最大下载速度（字节/秒）
    }
}
