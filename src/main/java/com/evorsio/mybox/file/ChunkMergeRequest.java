package com.evorsio.mybox.file;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkMergeRequest {
    @NotNull(message = "上传ID不能为空")
    private UUID uploadId;
}
