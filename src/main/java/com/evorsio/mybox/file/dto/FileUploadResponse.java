package com.evorsio.mybox.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadResponse {
    private String fileName;
    private boolean success;
    private String message; // 错误信息
}
