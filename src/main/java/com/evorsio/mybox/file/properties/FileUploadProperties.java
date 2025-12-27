package com.evorsio.mybox.file.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix="mybox.file.upload")
public class FileUploadProperties {
    private List<String> allowedExtensions;
    private List<String> allowedContentTypes;
}
