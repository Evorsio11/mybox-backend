package com.evorsio.mybox.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadChunkResponse {
    private Integer chunkNumber;
    private String status;
    private String chunkHash;
}
