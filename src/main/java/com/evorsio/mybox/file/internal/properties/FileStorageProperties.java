package com.evorsio.mybox.file.internal.properties;

import com.evorsio.mybox.common.SizeUnitParser;
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

    /**
     * 获取最大文件大小的字节数
     */
    public long getMaxFileSizeInBytes() {
        return SizeUnitParser.parseSizeToBytes(maxFileSize);
    }

    /**
     * 获取最大总大小的字节数
     */
    public long getMaxTotalSizeInBytes() {
        return SizeUnitParser.parseSizeToBytes(maxTotalSize);
    }

    /**
     * 获取保留时间的秒数
     */
    public long getRetentionInSeconds() {
        return SizeUnitParser.parseTimeToSeconds(retention);
    }
}
