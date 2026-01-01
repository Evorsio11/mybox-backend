package com.evorsio.mybox.file.internal.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mybox.file.download")
public class FileDownloadProperties {
    private boolean enableRateLimit = true;
    private String maxBytesPerSecond;
}
