package com.evorsio.mybox.file.internal.properties;

import com.evorsio.mybox.common.SizeUnitParser;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "mybox.file.download")
public class FileDownloadProperties {
    private boolean enableRateLimit = true;
    private String maxBytesPerSecond;

    /**
     * 获取每秒最大字节数
     */
    public long getMaxBytesPerSecondAsLong() {
        return SizeUnitParser.parseSizeToBytes(maxBytesPerSecond);
    }
}
