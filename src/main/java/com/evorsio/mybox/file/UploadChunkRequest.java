package com.evorsio.mybox.file;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadChunkRequest {
    @NotNull(message = "上传ID不能为空")
    private UUID uploadId;

    @NotNull(message = "分片编号不能为空")
    @Positive(message = "分片编号必须大于0")
    private Integer chunkNumber;

    @NotNull(message = "文件不能为空")
    private MultipartFile file;
}
