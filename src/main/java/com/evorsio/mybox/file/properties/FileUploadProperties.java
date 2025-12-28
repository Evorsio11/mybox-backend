package com.evorsio.mybox.file.properties;

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
    }
}
