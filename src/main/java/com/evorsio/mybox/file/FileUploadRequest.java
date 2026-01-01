package com.evorsio.mybox.file;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class FileUploadRequest {
    @NotEmpty(message = "文件列表不能为空")
    private List<MultipartFile> files;
}
