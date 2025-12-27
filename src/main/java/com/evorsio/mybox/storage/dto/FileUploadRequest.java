package com.evorsio.mybox.storage.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadRequest {
    @NotNull
    private MultipartFile file;
}
