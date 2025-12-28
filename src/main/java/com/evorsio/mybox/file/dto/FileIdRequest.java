package com.evorsio.mybox.file.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FileIdRequest {
    @NotNull(message = "文件ID不能为空")
    private UUID fileId;
}
