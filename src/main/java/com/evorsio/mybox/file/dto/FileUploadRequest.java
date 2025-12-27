package com.evorsio.mybox.file.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class FileUploadRequest {
    @NotEmpty
    private List<MultipartFile> files;
}
