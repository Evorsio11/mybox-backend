package com.evorsio.mybox.file.internal.properties;

import com.evorsio.mybox.common.SizeUnitParser;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "mybox.file.upload")
public class FileUploadProperties {
    private Types types = new Types();
    private Chunk chunk = new Chunk();

    @Data
    public static class Types {
        private boolean allowAll = false;
        private List<String> allowedContentTypes;
        private List<String> allowedExtensions;
    }

    @Data
    public static class Chunk {
        private boolean enabled = true;
        private String minSize = "100MB";
        private String chunkSize = "5MB";
        private boolean enableConcurrent = true;
        private int maxConcurrent = 4;
        private boolean enableDeduplication = true;
        private String deduplicationStrategy = "merge";
        private String sessionTimeout = "24h";
        private int maxRetry = 3;
        private String retryDelay = "5s";
        private boolean cleanupExpired = true;
        private String cleanupInterval = "1h";

        /**
         * 获取最小文件大小的字节数
         */
        public long getMinSizeInBytes() {
            return SizeUnitParser.parseSizeToBytes(minSize);
        }

        /**
         * 获取分片大小的字节数
         */
        public long getChunkSizeInBytes() {
            return SizeUnitParser.parseSizeToBytes(chunkSize);
        }

        /**
         * 获取会话超时的秒数
         */
        public long getSessionTimeoutInSeconds() {
            return SizeUnitParser.parseTimeToSeconds(sessionTimeout);
        }

        /**
         * 获取重试延迟的秒数
         */
        public long getRetryDelayInSeconds() {
            return SizeUnitParser.parseTimeToSeconds(retryDelay);
        }

        /**
         * 获取清理间隔的秒数
         */
        public long getCleanupIntervalInSeconds() {
            return SizeUnitParser.parseTimeToSeconds(cleanupInterval);
        }
    }
}
