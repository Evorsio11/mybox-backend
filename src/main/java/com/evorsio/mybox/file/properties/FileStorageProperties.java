package com.evorsio.mybox.file.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mybox.file.storage")
public class FileStorageProperties {
    private String tempDir;
    private String maxFileSize;
    private String maxTotalSize;
    private String retention;
}
