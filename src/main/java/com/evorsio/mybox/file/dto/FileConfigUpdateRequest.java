package com.evorsio.mybox.file.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件配置更新请求DTO（仅Admin可用）
 * 结构完全按照 YAML 配置层次设计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileConfigUpdateRequest {
    @Valid
    @NotNull(message = "存储配置不能为空")
    private StorageConfigUpdate storage;

    @Valid
    @NotNull(message = "上传配置不能为空")
    private UploadConfigUpdate upload;

    @Valid
    @NotNull(message = "下载配置不能为空")
    private DownloadConfigUpdate download;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageConfigUpdate {
        @NotBlank(message = "临时文件目录不能为空")
        private String tempDir;

        @NotNull(message = "最大文件大小不能为空")
        @Min(value = 1, message = "最大文件大小必须大于0")
        private Long maxFileSizeBytes;

        @NotNull(message = "最大总存储大小不能为空")
        @Min(value = 1, message = "最大总存储大小必须大于0")
        private Long maxTotalSizeBytes;

        @NotNull(message = "保留天数不能为空")
        @Min(value = 1, message = "保留天数必须大于0")
        private Integer retentionDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadConfigUpdate {
        @Valid
        @NotNull(message = "文件类型配置不能为空")
        private TypesConfigUpdate types;

        @Valid
        @NotNull(message = "分片上传配置不能为空")
        private ChunkConfigUpdate chunk;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TypesConfigUpdate {
            @NotNull(message = "允许所有文件类型不能为空")
            private Boolean allowAll;

            @NotNull(message = "允许的扩展名不能为空")
            private java.util.List<String> allowedExtensions;

            @NotNull(message = "允许的内容类型不能为空")
            private java.util.List<String> allowedContentTypes;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ChunkConfigUpdate {
            @NotNull(message = "启用分片上传不能为空")
            private Boolean enabled;

            @NotNull(message = "最小文件大小不能为空")
            @Min(value = 1, message = "最小文件大小必须大于0")
            private Long minSizeBytes;

            @NotNull(message = "分片大小不能为空")
            @Min(value = 1, message = "分片大小必须大于0")
            private Long chunkSizeBytes;

            @NotNull(message = "启用并发上传不能为空")
            private Boolean enableConcurrent;

            @NotNull(message = "最大并发数不能为空")
            @Min(value = 1, message = "最大并发数必须大于0")
            private Integer maxConcurrent;

            @NotNull(message = "启用去重不能为空")
            private Boolean enableDeduplication;

            @NotNull(message = "去重策略不能为空")
            @NotBlank(message = "去重策略不能为空")
            private String deduplicationStrategy;

            @NotNull(message = "会话超时时间不能为空")
            @Min(value = 1, message = "会话超时时间必须大于0")
            private Integer sessionTimeoutHours;

            @NotNull(message = "最大重试次数不能为空")
            @Min(value = 0, message = "最大重试次数不能为负数")
            private Integer maxRetry;

            @NotNull(message = "重试延迟不能为空")
            @Min(value = 0, message = "重试延迟不能为负数")
            private Integer retryDelaySeconds;

            @NotNull(message = "清理过期会话不能为空")
            private Boolean cleanupExpired;

            @NotNull(message = "清理间隔不能为空")
            @Min(value = 1, message = "清理间隔必须大于0")
            private Integer cleanupIntervalHours;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadConfigUpdate {
        @NotNull(message = "启用下载限速不能为空")
        private Boolean enableRateLimit;

        @NotNull(message = "最大下载速度不能为空")
        @Min(value = 0, message = "最大下载速度不能为负数")
        private Long maxBytesPerSecondBytes;
    }
}
